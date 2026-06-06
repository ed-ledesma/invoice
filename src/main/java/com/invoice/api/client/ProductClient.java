package com.invoice.api.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.invoice.api.dto.DtoCartItem;
import com.invoice.api.dto.DtoProduct;

@Component
public class ProductClient {

    @Autowired
    private WebClient productWebClient;

    // GET /cart-item lista los items del carrito del usuario autenticado
    public List<DtoCartItem> getCartItems(String token) {
        return productWebClient.get()
                .uri("/cart-item")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DtoCartItem>>() {})
                .block();
    }

    // GET /product/gtin/{gtin} obtiene precio y stock del producto
    public DtoProduct getProductByGtin(String gtin, String token) {
        return productWebClient.get()
                .uri("/product/gtin/{gtin}", gtin)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(DtoProduct.class)
                .block();
    }

    // PATCH /product/gtin/{gtin}/stock?quantity=N  descuenta stock
    public void decreaseStock(String gtin, Integer quantity, String token) {
        productWebClient.patch()
                .uri("/product/gtin/{gtin}/stock?quantity={quantity}", gtin, quantity)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    // DELETE /cart-item vacía el carrito del usuario autenticado
    public void emptyCart(String token) {
        productWebClient.delete()
                .uri("/cart-item")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}