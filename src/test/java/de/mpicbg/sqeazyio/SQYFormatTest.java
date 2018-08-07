/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package de.mpicbg.sqeazyio;

import io.scif.img.IO;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.ImageMetadata;
import io.scif.FormatException;
import io.scif.ByteArrayPlane;

import java.net.URL;
import java.io.IOException;
import java.util.Arrays;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.scijava.Context;

import de.mpicbg.sqeazyio.SqeazyFormat;
import de.mpicbg.sqeazyio.SqeazyFormat.Parser;
import de.mpicbg.sqeazyio.SqeazyFormat.Reader;
import de.mpicbg.sqeazyio.SqeazyFormat.Metadata;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;

public class SQYFormatTest {

   private static final Context context = new Context();
	private static final SqeazyFormat format = new SqeazyFormat();
	private static SqeazyFormat.Reader reader;
	private static SqeazyFormat.Parser parser;
	// private static final SqeazyFormat.Checker checker =
	// 	new SqeazyFormat.Checker();

    @BeforeClass
	public static void oneTimeSetup() throws Exception {
		format.setContext(context);
		reader = (SqeazyFormat.Reader) format.createReader();
		parser = (SqeazyFormat.Parser) format.createParser();
	}

	@Before
	public void setUp() throws Exception {}

	@AfterClass
	public static void oneTimeTearDown() {
		context.dispose();
	}

    @Test public void testFormatNameMethod() {

        SqeazyFormat f = new SqeazyFormat();

        String obs = f.getFormatName();
        final String exp = "sqeazy";
        assertEquals(exp,obs);
    }

    @Test
	public void testOpenPlane() throws Exception {
		// SETUP
        final URL tiny = getClass().getResource("flybrain.sqy");
        assertNotEquals(tiny,null);

        final String fpath = tiny.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/flybrain.sqy"));

        final short width = 256;
		final short height = 256;
		final int planeBytes = width * height * 2;

		final Interval bounds = new FinalInterval(width, height);
		final ByteArrayPlane plane = new ByteArrayPlane(context);
		plane.setData(new byte[planeBytes]);
        Arrays.fill(plane.getData(), 0, planeBytes, (byte) 42);

		final RandomAccessInputStream stream = new RandomAccessInputStream(context,fpath);
		final Reader reader = (Reader) format.createReader();

        assertNotEquals(stream, null);
		assertNotEquals(reader, null);
		reader.setSource(stream);

		// EXECUTE
		reader.openPlane(0, (long)0, plane, // bounds,
                         new SCIFIOConfig());

		// VERIFY
		assertNotEquals(plane.getData()[0],42);
	}

}
