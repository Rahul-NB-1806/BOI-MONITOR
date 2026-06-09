package com.boi.monitor;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.boi.monitor.parser.NotificationParser;
import com.boi.monitor.model.ParsedNotification;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented tests that run on an Android device or emulator.
 * These verify the parser and filter in the real Android runtime.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.boi.monitor", appContext.getPackageName());
    }

    @Test
    public void parser_upiCreditRecognizedOnDevice() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 999 on 01/01/2026 " +
                     "and debited from a/c no. XXXXXX1111 (UPI Ref noINSTRTest001)";

        assertTrue(NotificationParser.passesFilter(raw));
        ParsedNotification result = NotificationParser.parse(raw);
        assertEquals(ParsedNotification.NotificationType.UPI_CREDIT, result.getType());
        assertEquals(999.0, result.getAmount(), 0.01);
    }

    @Test
    public void parser_filterRejectsNonBOI() {
        String raw = "HDFC - Transaction for your account ending 1234, amount Rs 500";
        assertFalse(NotificationParser.passesFilter(raw));
    }
}
