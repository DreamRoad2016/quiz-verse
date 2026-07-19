package net.quizverse.pack;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PackNotFoundException extends RuntimeException {

    public PackNotFoundException(String packId) {
        super("Pack not found: " + packId);
    }
}
