package io.yayotron.investmentassistant.model;

@FunctionalInterface
public interface Assistant {
    String answer(String query);
}