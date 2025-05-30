package com.florescer.product.domain.exception;

public record ApiErrorResponse(String error, Object details) {}