package com.example.lab1;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.function.Predicate;

// https://linuxtut.com/en/c958f5fc0432efdae508/
// https://gist.github.com/GuiSim/e1d1cde0ab66302ae45c
public class LambdaMatcher<T> extends TypeSafeMatcher<T> {

    private final Predicate<T> predicate;

    private final String message;

    public LambdaMatcher(Predicate<T> predicate) {
        this(predicate, null);
    }

    public LambdaMatcher(Predicate<T> predicate, String message) {
        this.predicate = predicate;
        this.message = message;
    }

    @Override
    public void describeTo(Description description) {
        if (message != null) {
            description.appendText(this.message);
        }
    }

    @Override
    protected boolean matchesSafely(T item) {
        return this.predicate.test(item);
    }

    public static <T> Matcher<T> matches(Predicate<T> predicate) {
        return new LambdaMatcher<>(predicate);
    }

    public static <T> Matcher<T> matches(Predicate<T> predicate, String message) {
        return new LambdaMatcher<>(predicate, message);
    }
}
