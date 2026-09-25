package com.lexicon.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcessTextRequest(@NotBlank String text) {
}
