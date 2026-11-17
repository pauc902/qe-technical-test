package com.example.qe;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Throwaway test that proves the toolchain works: Maven -> JUnit -> our code -> AssertJ. */
class SmokeTest {

    @Test
    void toolchainWorks() {
        int result = 1 + 1;

        assertThat(result).isEqualTo(2);
    }
}
