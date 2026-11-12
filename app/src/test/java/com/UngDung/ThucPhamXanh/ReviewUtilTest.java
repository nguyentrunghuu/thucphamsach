package com.UngDung.ThucPhamXanh;

import static org.junit.Assert.assertEquals;

import com.UngDung.ThucPhamXanh.Helper.ReviewUtil;

import org.junit.Test;

public class ReviewUtilTest {

    @Test
    public void parseRate_handlesStoredFormats() {
        assertEquals(5f, ReviewUtil.parseRate("5"), 0f);
        assertEquals(4f, ReviewUtil.parseRate("4.0"), 0f);
        assertEquals(2.5f, ReviewUtil.parseRate("2.5"), 0f);
        assertEquals(3.5f, ReviewUtil.parseRate("3,5"), 0f);
    }

    @Test
    public void parseRate_invalidValuesBecomeZeroOrClamped() {
        assertEquals(0f, ReviewUtil.parseRate(null), 0f);
        assertEquals(0f, ReviewUtil.parseRate(""), 0f);
        assertEquals(0f, ReviewUtil.parseRate("abc"), 0f);
        assertEquals(0f, ReviewUtil.parseRate("NaN"), 0f);
        assertEquals(5f, ReviewUtil.parseRate("9"), 0f);
        assertEquals(0f, ReviewUtil.parseRate("-1"), 0f);
    }

    @Test
    public void starBucket_roundsUpIntoOneToFive() {
        assertEquals(1, ReviewUtil.starBucket(0f));
        assertEquals(1, ReviewUtil.starBucket(1f));
        assertEquals(3, ReviewUtil.starBucket(2.5f));
        assertEquals(4, ReviewUtil.starBucket(4f));
        assertEquals(5, ReviewUtil.starBucket(4.5f));
        assertEquals(5, ReviewUtil.starBucket(5f));
    }

    @Test
    public void formatRate_oneDecimal() {
        assertEquals("0.0", ReviewUtil.formatRate(0f));
        assertEquals("4.5", ReviewUtil.formatRate(4.5f));
        assertEquals("4.3", ReviewUtil.formatRate(13f / 3f));
    }
}
