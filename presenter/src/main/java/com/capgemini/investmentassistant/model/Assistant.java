package com.capgemini.investmentassistant.model;

@FunctionalInterface
public interface Assistant {
    String answer(String query);
}