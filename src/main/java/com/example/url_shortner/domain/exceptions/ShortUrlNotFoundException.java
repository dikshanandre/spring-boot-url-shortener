package com.example.url_shortner.domain.exceptions;

public class ShortUrlNotFoundException extends RuntimeException{
    public  ShortUrlNotFoundException(String message){
        super(message);
    }
}
