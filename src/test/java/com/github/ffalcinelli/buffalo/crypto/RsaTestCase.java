package com.github.ffalcinelli.buffalo.crypto;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by fabio on 28/02/17.
 */
public class RsaTestCase {

    private Rsa rsa;

    @Before
    public void setUp() {

        rsa = new Rsa("65537",
                "A5261939975948BB7A58DFFE5FF54E65F0498F9175F5A09288810B8975871E99" +
                        "AF3B5DD94057B0FC07535F5F97444504FA35169D461D0D30CF0192E307727C06" +
                        "5168C788771C561A9400FB49175E9E6AA4E23FE11AF69E9412DD23B0CB6684C4" +
                        "C2429BCE139E848AB26D0829073351F4ACD36074EAFD036A5EB83359D2A698D3");
    }

    @Test
    public void base64() {
        assertEquals("", rsa.hexToBase64(""));
        assertEquals("ASNFZ4mrze8=", rsa.hexToBase64("0123456789ABCDEF"));
    }

    //TODO: test with decrypt logic
    @Test
    public void encrypt() {
        assertNotEquals("this is a test string", rsa.encrypt("this is a test string"));
        assertNotEquals("", rsa.encrypt("çÇ"));
        assertNotEquals("", rsa.encrypt(String.valueOf(Character.toChars(0xFB4))));
        assertNotEquals("", rsa.encrypt("1234"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void messageTooLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 512 + 12; i++) {
            sb.append("a");
        }
        rsa.encrypt(sb.toString());
    }


}
