package org.gridsuite.useradmin.server;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

class CoverageTest implements WithAssertions {
    @Test
    void testUtilityClassConstructor() {
        assertThatThrownBy(Utils.class::newInstance).as("Utils class init exception")
                .isInstanceOf(IllegalAccessException.class);
    }

}
