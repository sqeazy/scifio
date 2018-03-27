/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package de.mpicbg.sqeazyio;

import io.scif.img.IO;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;

import java.net.URL;
import java.io.IOException;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.scijava.Context;

import org.bridj.Pointer;
import org.bridj.CLong;
import static org.bridj.Pointer.*;
import sqeazy.bindings.SqeazyLibrary;

import de.mpicbg.sqeazyio.SqeazyFormat.Checker;

public class SQYCheckerTest {

    private Context context;
    private Checker c;

    @Before
    public void setUp() {

        context = new Context();
        //final SCIFIO scifio = new SCIFIO();
        // final Format f = scifio.format().getFormat(id);
        // c = f.createChecker();
        c = new Checker();
        c.setContext(context);
    }

    @After
	public void tearDown() {
		context = null;
		c = null;
	}

    @Test public void testType() {

        assertEquals(c.suffixSufficient(),true);
    }

    @Test public void testPipelineContained() throws IOException {

        final URL tiny = getClass().getResource("tiny-10x10x3.sqy");
        assertNotEquals(tiny,null);

        final String fpath = tiny.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/tiny-10x10x3.sqy"));

        final RandomAccessInputStream stream = new RandomAccessInputStream(context, fpath);
        final int blockLen = 16 << 10;
        //if (!FormatTools.validStream(stream, blockLen, false)) return false;
        final String data = stream.readString(blockLen);
        assertThat(data, containsString("pipename"));
        assertThat(data, containsString("rank"));

    }

    @Test public void testHeaderCall() throws IOException {

        final URL tiny = getClass().getResource("tiny-10x10x3.sqy");
        assertNotEquals(tiny,null);

        final String fpath = tiny.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/tiny-10x10x3.sqy"));

        final RandomAccessInputStream stream = new RandomAccessInputStream(context, fpath);
        final int blockLen = 16 << 10;
        //if (!FormatTools.validStream(stream, blockLen, false)) return false;
        final String data = stream.readString(blockLen);
        assertThat(data, containsString("pipename"));
        assertThat(data, containsString("rank"));

        final Pointer<Byte> bHdr = pointerToCString(data);
        final Pointer<CLong> lLength = Pointer.allocateCLong().setLong(0);
        final int iRValue = SqeazyLibrary.SQY_Header_Size(bHdr,lLength);

        assertEquals(iRValue,0);
    }

    @Test public void testRecognizesFormat() throws IOException {

        final URL tiny = getClass().getResource("tiny-10x10x3.sqy");
        assertNotEquals(tiny,null);

        final String fpath = tiny.getPath();
        assertThat(fpath, containsString("de/mpicbg/sqeazyio/tiny-10x10x3.sqy"));

        final RandomAccessInputStream file_stream = new RandomAccessInputStream(context, fpath);

        assertNotEquals(c,null);
        assertNotEquals(file_stream,null);

        assertEquals(c.isFormat(file_stream),true);
    }


}
