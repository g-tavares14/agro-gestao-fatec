package com.agrogestao.common;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class FileNames {

    private FileNames() {
    }

    public static String sanitize(String original) {
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        String name;
        try {
            name = Path.of(original).getFileName().toString();
        } catch (InvalidPathException ex) {
            name = original.replace('\\', '/');
            int slash = name.lastIndexOf('/');
            name = slash >= 0 ? name.substring(slash + 1) : name;
        }
        name = name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return name.isBlank() ? "arquivo" : name;
    }
}
