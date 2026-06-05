package com.invoice.api.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.invoice.api.dto.ApiResponse;
import com.invoice.api.dto.DtoInvoiceList;
import com.invoice.api.entity.Invoice;
import com.invoice.api.repository.RepoInvoice;
import com.invoice.commons.mapper.MapperInvoice;
import com.invoice.commons.util.JwtDecoder;
import com.invoice.exception.ApiException;
import com.invoice.exception.DBAccessException;

@Service
public class SvcInvoiceImp implements SvcInvoice {
	
	@Autowired
    private RepoInvoice repo;
	
	@Autowired
	private JwtDecoder jwtDecoder;
	
	@Autowired
	MapperInvoice mapper;

	@Override
	public List<DtoInvoiceList> findAll() {
		try {
			if(jwtDecoder.isAdmin()) {
				return mapper.toDtoList(repo.findAll());
			}else {
				Integer user_id = jwtDecoder.getUserId();
				return mapper.toDtoList(repo.findAllByUserId(user_id));
			}
		}catch (DataAccessException e) {
	        throw new DBAccessException();
	    }
	}

	@Override
	public Invoice findById(Integer id) {
		try {
			Invoice invoice = repo.findById(id).get();
			if(!jwtDecoder.isAdmin()) {
				Integer user_id = jwtDecoder.getUserId();
				if(invoice.getUser_id() != user_id) {
					throw new ApiException(HttpStatus.FORBIDDEN, "El token no es válido para consultar esta factura");
				}
			}
			return invoice;
		}catch (DataAccessException e) {
	        throw new DBAccessException();
	    }catch (NoSuchElementException e) {
			throw new ApiException(HttpStatus.NOT_FOUND, "El id de la factura no existe");
	    }
	}

	@Override
	public ApiResponse create() {
		try {
			/*
			 * Proyecto: Requerimiento 2 - Finalizar compra
			 */
			return new ApiResponse("La factura ha sido registrada");
		}catch (DataAccessException e) {
	        throw new DBAccessException();
	    }
	}
}
