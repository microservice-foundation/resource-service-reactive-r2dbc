package com.epam.training.microservicefoundation.resourceservice.service;

public interface Convertor<O, I> {
    O covert(I input);
}
