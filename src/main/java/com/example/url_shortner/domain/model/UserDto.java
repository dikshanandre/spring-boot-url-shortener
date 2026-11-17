package com.example.url_shortner.domain.model;


import java.io.Serializable;

public record UserDto(Long id, String name) implements Serializable {
}