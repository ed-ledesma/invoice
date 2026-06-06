package com.invoice.api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.invoice.api.client.ProductClient;
import com.invoice.api.dto.ApiResponse;
import com.invoice.api.dto.DtoCartItem;
import com.invoice.api.dto.DtoInvoiceList;
import com.invoice.api.dto.DtoProduct;
import com.invoice.api.entity.Invoice;
import com.invoice.api.entity.InvoiceItem;
import com.invoice.api.repository.RepoInvoice;
import com.invoice.commons.mapper.MapperInvoice;
import com.invoice.commons.util.JwtDecoder;
import com.invoice.exception.ApiException;
import com.invoice.exception.DBAccessException;

@Service
public class SvcInvoiceImp implements SvcInvoice {

    private static final double TAX_RATE = 0.16;

    @Autowired
    private RepoInvoice repo;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private MapperInvoice mapper;

    @Autowired
    private ProductClient productClient;

    @Override
    public List<DtoInvoiceList> findAll() {
        try {
            if (jwtDecoder.isAdmin()) {
                return mapper.toDtoList(repo.findAll());
            } else {
                Integer user_id = jwtDecoder.getUserId();
                return mapper.toDtoList(repo.findAllByUserId(user_id));
            }
        } catch (DataAccessException e) {
            throw new DBAccessException();
        }
    }

    @Override
    public Invoice findById(Integer id) {
        try {
            Invoice invoice = repo.findById(id).get();
            if (!jwtDecoder.isAdmin()) {
                Integer user_id = jwtDecoder.getUserId();
                if (invoice.getUser_id() != user_id) {
                    throw new ApiException(HttpStatus.FORBIDDEN,
                            "El token no es válido para consultar esta factura");
                }
            }
            return invoice;
        } catch (DataAccessException e) {
            throw new DBAccessException();
        } catch (NoSuchElementException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El id de la factura no existe");
        }
    }

    @Override
    @Transactional
    public ApiResponse create(String token) {
        try {
            Integer userId = jwtDecoder.getUserId();

            // ── 1. Obtener artículos del carrito ──────────────────────────────
            List<DtoCartItem> cartItems = productClient.getCartItems(token);

            if (cartItems == null || cartItems.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El carrito está vacío");
            }

            // ── 2. Validar stock y construir InvoiceItems ─────────────────────
            String invoiceId = UUID.randomUUID().toString();
            List<InvoiceItem> invoiceItems = new ArrayList<>();
            double invoiceTotal = 0.0;

            for (DtoCartItem cartItem : cartItems) {

                DtoProduct product = productClient.getProductByGtin(cartItem.getGtin(), token);

                if (product == null) {
                    throw new ApiException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado: " + cartItem.getGtin());
                }

                if (product.getStock() < cartItem.getQuantity()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Stock insuficiente para: " + product.getProduct()
                            + ". Disponible: " + product.getStock()
                            + ", solicitado: " + cartItem.getQuantity());
                }

                // ── 3. Calcular totales por artículo ──────────────────────────
                //  total    = precio_unitario × cantidad
                //  taxes    = total × 0.16
                //  subtotal = total − taxes
                double itemTotal    = Math.round(product.getPrice() * cartItem.getQuantity() * 100.0) / 100.0;
                double itemTaxes    = Math.round(itemTotal * TAX_RATE * 100.0) / 100.0;
                double itemSubtotal = Math.round((itemTotal - itemTaxes) * 100.0) / 100.0;

                invoiceItems.add(new InvoiceItem(
                        UUID.randomUUID().toString(),        // invoice_item_id
                        invoiceId,                           // invoice_id
                        cartItem.getGtin(),                  // gtin
                        cartItem.getQuantity(),              // quantity
                        product.getPrice().doubleValue(),    // unit_price
                        itemSubtotal,                        // subtotal (sin IVA)
                        itemTaxes,                           // taxes
                        itemTotal                            // total (con IVA)
                ));

                invoiceTotal += itemTotal;
            }

            // Redondear totales globales
            invoiceTotal            = Math.round(invoiceTotal * 100.0) / 100.0;
            double invoiceTaxes     = Math.round(invoiceTotal * TAX_RATE * 100.0) / 100.0;
            double invoiceSubtotal  = Math.round((invoiceTotal - invoiceTaxes) * 100.0) / 100.0;

            // ── 4. Guardar factura (cascade guarda invoice_items también) ─────
            String createdAt = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            repo.save(new Invoice(
                    invoiceId,
                    userId,
                    createdAt,
                    invoiceSubtotal,
                    invoiceTaxes,
                    invoiceTotal,
                    invoiceItems
            ));

            // ── 5. Descontar stock en API Product ─────────────────────────────
            for (DtoCartItem cartItem : cartItems) {
                productClient.decreaseStock(cartItem.getGtin(), cartItem.getQuantity(), token);
            }

            // ── 6. Vaciar carrito en API Product ──────────────────────────────
            productClient.emptyCart(token);

            return new ApiResponse("La factura ha sido registrada");

        } catch (ApiException e) {
            throw e;
        } catch (DataAccessException e) {
            throw new DBAccessException();
        }
    }
}
