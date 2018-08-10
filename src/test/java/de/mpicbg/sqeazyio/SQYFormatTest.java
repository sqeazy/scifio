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
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

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
	public void testUI8Plane() throws Exception {
		// SETUP
        final URL flybrain = getClass().getResource("flybrain.sqy");
        assertNotEquals(flybrain,null);

        final String fpath = flybrain.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/flybrain.sqy"));

        final int width = 256;
		final int height = 256;
		final int planeBytes = width * height;

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
        //first plane is all zeros
        ByteBuffer read_plane = ByteBuffer.wrap(plane.getData());
		assertNotEquals(read_plane.get(0),(byte)42);
        assertEquals(read_plane.get(0),(byte)0);
        assertEquals(read_plane.get((width*height) - 1),(byte)0);

        //try to extract plane 24
        plane.setData(new byte[planeBytes]);
        Arrays.fill(plane.getData(), 0, planeBytes, (byte) 42);
        reader.openPlane(0, (long)24, plane, // bounds,
                         new SCIFIOConfig());

        read_plane = ByteBuffer.wrap(plane.getData());
        assertNotEquals(read_plane.get(0),(byte)42);
        assertEquals(read_plane.get(0),(byte)0);

        //intensity(z=24, y=3, x=107) == 79
        assertEquals(plane.getData()[3*width + 107],(byte)79);
        assertEquals(read_plane.get(3*width + 107),(byte)79);

        //intensity(z=24, y=16, x=112) == 121
        assertEquals(plane.getData()[16*width + 112],(byte)121);
        assertEquals(read_plane.get(16*width + 112),(byte)121);

        //intensity(z=24, y=232, x=188) == 152


	}

    @Test
	public void testUI16Plane() throws Exception {
		// SETUP
        final URL tiny = getClass().getResource("droso.sqy");
        assertNotEquals(tiny,null);

        final String fpath = tiny.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/droso.sqy"));

        final int width = 64;
		final int height = 64;
		final int planeBytes = width * height * 2;

		final Interval bounds = new FinalInterval(width, height);
		final ByteArrayPlane plane = new ByteArrayPlane(context);
		plane.setData(new byte[planeBytes]);
        Arrays.fill(plane.getData(), 0, planeBytes, (byte) 16);

		final RandomAccessInputStream stream = new RandomAccessInputStream(context,fpath);
		final Reader reader = (Reader) format.createReader();

        assertNotEquals(stream, null);
		assertNotEquals(reader, null);
		reader.setSource(stream);

		// EXECUTE
        final byte origin = plane.getBytes()[0];

		reader.openPlane(0, (long)0, plane, // bounds,
                         new SCIFIOConfig());

        assertNotEquals(plane.getBytes()[0],origin);
		// VERIFY, first 3 pixels are 100, 101, 96 as 16-bit values
        ByteBuffer read_plane = ByteBuffer.wrap(plane.getBytes()).order(ByteOrder.nativeOrder());

        //assertEquals((byte)0,plane.getBytes()[0]);
        assertEquals(planeBytes,read_plane.capacity());
        assertEquals((byte)100,read_plane.get(0));
        assertEquals((byte)0,read_plane.get(1));
        assertEquals((byte)101,read_plane.get(2));
        assertEquals((byte)0,read_plane.get(3));
        assertEquals((byte)96,read_plane.get(4));
        assertEquals((byte)0,read_plane.get(5));

        //assertEquals((short)100,read_plane.asShortBuffer().get(0));

        final ShortBuffer read_shorts = read_plane.asShortBuffer();
        assertEquals(planeBytes/2,read_shorts.capacity());
		assertEquals((short)100,read_shorts.get(0));
        assertEquals((short)101,read_shorts.get(1));
        assertEquals((short)96,read_shorts.get(2));
        assertNotEquals((short)151,read_plane.asShortBuffer().get(50*width + 33));

        // EXECUTE
		reader.openPlane(0, (long)48, plane, // bounds,
                         new SCIFIOConfig());
        read_plane = ByteBuffer.wrap(plane.getBytes()).order(ByteOrder.nativeOrder());
        assertNotEquals((short)100,read_plane.asShortBuffer().get(0));
        assertEquals((short)151,read_plane.asShortBuffer().get(50*width + 33));

	}

}
