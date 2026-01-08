package ax.gritlab.buy_01.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * INTENTIONAL FAILING TEST - For testing Jenkins pipeline failure behavior
 * DELETE THIS FILE after testing!
 */
public class FailingTest {

    @Test
    @DisplayName("❌ INTENTIONAL FAIL - Delete this test after verifying pipeline behavior")
    void testThatShouldFail() {
        // This test is designed to fail on purpose
        // to demonstrate how Jenkins handles test failures
        
        int expected = 42;
        int actual = 0;  // Wrong value!
        
        assertEquals(expected, actual, 
            "This test is INTENTIONALLY failing to test Jenkins pipeline behavior. " +
            "Delete FailingTest.java after testing!");
    }
}
