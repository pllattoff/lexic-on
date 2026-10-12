package com.lexicon.backend.dto;

import java.util.List;

public record AssignedLemmasResponse(
        List<String> lemmas
) {
}
