package de.mpicbg.sqeazyio;

import io.scif.AbstractChecker;
import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.AbstractTranslator;
import io.scif.AbstractWriter;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.Field;
import io.scif.Format;
import io.scif.util.FormatTools;
import io.scif.FormatException;
import io.scif.HasColorTable;
import io.scif.ImageMetadata;
import io.scif.Plane;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.io.RandomAccessOutputStream;
import io.scif.services.FormatService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.imagej.axis.Axes;
import org.bridj.Pointer;
import org.bridj.CLong;

import static org.bridj.Pointer.*;

import sqeazy.bindings.SqeazyLibrary;

import org.scijava.plugin.Plugin;

/**
 * The {@link Format} class itself has three purposes:
 * <ul>
 * <li>Act as the unit of discovery, via the SciJava plugin mechanism</li>
 * <li>Define the name and extension(s) of the format</li>
 * <li>Define and generate the functional SCIFIO component classes</li>
 * </ul>
 * <p>
 * In this tutorial, we'll create a non-functional Format which adds "support"
 * for a fictional ".scifiosmpl" image type.
 * </p>
 * <p>
 * There are six potential component types in a {@code Format}:
 * <ol>
 * <li>Metadata</li>
 * <li>Checker</li>
 * <li>Parser</li>
 * <li>Reader</li>
 * <li>Writer</li>
 * <li>Translator(s)</li>
 * </ol>
 * We will cover each here, and explain if and why you would implement each.
 * </p>
 * <p>
 * See the {@link FormatService} plugin deals for general methods managing
 * Formats within a given context.
 * </p>
 *
 * @author Mark Hiner
 */
// NB: The Plugin annotation here allows the format to be discovered
// automatically - satisfying the first role of the Format
@Plugin(type = Format.class)
public class SqeazyFormat extends AbstractFormat {

    // *** FORMAT API ***
    // A lot of work is done for you in the AbstractFormat and Abstact component
    // classes. The two methods left to implement identify the Format and its
    // supported extensions.

    // First we have to declare a name for our Format
    @Override
    public String getFormatName() {
        return "sqeazy";
    }

    // Then we need to register what suffix(es) the Format is capable of
    // opening.
    // NB: you shouldn't put a leading separator ('.') in the extension Strings.
    @Override
    protected String[] makeSuffixArray() {
        return new String[] { "sqy" };
    }

    // *** REQUIRED COMPONENTS ***

    // The Metadata class contains all format-specific metadata.
    // Your Metadata class should be filled with fields which define the
    // image format. For example, things like acquisition date, instrument,
    // excitation levels, etc.
    // In the implementation of populateImageMetadata, the format- specific
    // metadata is converted to a generalized ImageMetadata which can be
    // consumed by other components (e.g. readers/writers).
    // As the conversion to ImageMetadata is almost certainly lossy, preserving
    // the original format-specific metadata provides components like
    // Translators an opportunity to preserve as much original information
    // as possible.
    //
    // NB: if your format has a color table/LUT which you would like to expose,
    // it should implement the io.scif.HasColorTable interface.
    public static class Metadata extends AbstractMetadata {

        // Each format-specific field in your Metadata class should be private,
        // with a public accessor, and mutator if necessary.
        // These fields should be annotated with io.scif.Field notations, and a
        // label indicating the field's original name (as it might not be properly
        // represented by Java camelCase naming conventions).
        @Field(label = "pixel type descriptor")
        private String type;

        public void setType(final String c) {
            type = c;
        }

        public final String getType() {
            return type;
        }

        @Field(label = "compression pipeline")
        private String pipeline;

        public void setPipeline(final String c) {
            pipeline = c;
        }

        public final String getPipeline() {
            return pipeline;
        }

        		/**
		 * Because we have no way of indexing into the text file efficiently in
		 * general, we cheat and store the entire file's data in a giant array.
		 */
		private byte[] data;
        //private float[][] data;

		/** Current row number. */
		private int row;

		/** Number of tokens per row. */
		private int rowLength;

		/** Column index for X coordinate. */
		private int xIndex = -1;

		/** Column index for Y coordinate. */
		private int yIndex = -1;

		/** List of channel labels. */
		private String[] channels;

		/** Image width. */
        @Field(label = "width")
		private int sizeX;

		/** Image height. */
		@Field(label = "heigth")
		private int sizeY;

        /** Image width. */
		@Field(label = "depth")
		private int sizeZ;

		// -- TextMetadata getters and setters --

		public byte[] getData() {
			return data;
		}

		public void setData(final byte[] data) {
			this.data = data;

		}

		public int getRow() {
			return row;
		}

		public void setRow(final int row) {
			this.row = row;
		}

		public int getRowLength() {
			return rowLength;
		}

		public void setRowLength(final int rowLength) {
			this.rowLength = rowLength;
		}

		public int getxIndex() {
			return xIndex;
		}

		public void setxIndex(final int xIndex) {
			this.xIndex = xIndex;
		}

		public int getyIndex() {
			return yIndex;
		}

		public void setyIndex(final int yIndex) {
			this.yIndex = yIndex;
		}

		public String[] getChannels() {
			return channels;
		}

		public void setChannels(final String[] channels) {
			this.channels = channels;
		}

		public int getSizeX() {
			return sizeX;
		}

		public void setSizeX(final int sizeX) {
			this.sizeX = sizeX;
		}

		public int getSizeY() {
			return sizeY;
		}

        public int getSizeZ() {
			return sizeZ;
		}

		public void setSizeY(final int sizeY) {
			this.sizeY = sizeY;
		}

        public void setSizeZ(final int sizeZ) {
			this.sizeZ = sizeZ;
		}

		// -- Metadata API Methods --

		@Override
		public void populateImageMetadata() {

            //we should extract the header here from this->data
            //then we could populate the

			final ImageMetadata iMeta = get(0);

			iMeta.setPlanarAxisCount(3);//2?
			iMeta.setPixelType(FormatTools.UINT16);//decide this based on type
			iMeta.setBitsPerPixel(16);
			iMeta.setOrderCertain(true);
			iMeta.setLittleEndian(true);//assuming the data was produced under x86 predominantly
			iMeta.setMetadataComplete(true);
		}

		@Override
		public void close(final boolean fileOnly) throws IOException {
			super.close(fileOnly);
			if (!fileOnly) {
				data = null;
				rowLength = 0;
				xIndex = yIndex = -1;
				channels = null;
				sizeX = sizeY = sizeZ = 0;
				row = 0;
			}
		}


    }


    public static class Checker extends AbstractChecker {

		// -- Checker API Methods --

		@Override
		public boolean suffixSufficient() {
			return true;
		}

		@Override
		public boolean isFormat(final RandomAccessInputStream stream)
			throws IOException
		{
			final int blockLen = 16 << 10;
			if (!FormatTools.validStream(stream, blockLen, false)) return false;
			final String data = stream.readString(blockLen);
			// final List<String> lines = Arrays.asList(data.split("\n"));

            final Pointer<Byte> bHdr = pointerToCString(data);
            final Pointer<CLong> lLength = Pointer.allocateCLong().setLong(0);
            final int iRValue = SqeazyLibrary.SQY_Header_Size(bHdr,lLength);

            System.out.println("\n>> SqeazyLibrary.SQY_Header_Size called, detected size "+lLength.getInt()+", return value "+iRValue+"\n");

            if(lLength.getInt() != 0)
                return true;

            Metadata meta = null;
			try {
				meta = (Metadata) getFormat().createMetadata();
			}
			catch (final FormatException e) {
				log().error("Failed to create SqeazyMetadata", e);
				return false;
			}

			meta.createImageMetadata(1);
			// meta.setRow(0);

			// final String[] line = TextUtils.getNextLine(lines, meta);
			// if (line == null) return false;
			// int headerRows = 0;
			// try {
			// 	headerRows = TextUtils.parseFileHeader(lines, meta, log());
			// }
			// catch (final FormatException e) {}
			return false;
		}
	}
//     // The Parser is your interface with the image source.
//     // It has one purpose: to take the raw image information and generate a
//     // Metadata instance, populating all format-specific fields.
//     public static class Parser extends AbstractParser<Metadata> {

//         // In this method we populate the given Metadata object
//         @Override
//         public void typedParse(final RandomAccessInputStream stream,
//                                final Metadata meta, final SCIFIOConfig config) throws IOException,
//             FormatException
// 			{
// 				meta.setColor("blue");

// 				// The provided SCIFIOConfig object has a number of configuration
// 				// options we may want to read at this point.
// 				// Any SCIFIOConfig#parser[XXXX] method is intended for this stage.

// 				// If the MetadataLevel is MINIMUM, you only need to populate the
// 				// minimum needed for generating ImageMetadata - extra "fluff" can
// 				// be skipped. Note that this is more of a performance option - if
// 				// ignored your format should still work, it just may be slower than
// 				// optimal in some circumstances.
// 				config.parserGetLevel();

// 				// This is state flag that gets passed to the Metadata automatically.
// 				config.parserIsFiltered();

// 				// If this flag is set, it signifies a desire to save the raw original
// 				// metadata, beyond what is represented in this format's fields.
// 				config.parserIsSaveOriginalMetadata();
// 			}
//     }

//     // The purpose of the Checker is to determine if an image source is
//     // compatible with this Format.
//     // If you just want to use basic extension checking to determine
//     // compatibility, you do not need to implement any methods in this class -
//     // it's already handled for you in the Abstract layer.
//     //
//     // However, if your format embeds an identifying flag - e.g. a magic string
//     // or number - then it should override suffixSufficient, suffixNecessary
//     // and isFormat(RandomAccessInputStream) as appropriate.
//     public static class Checker extends AbstractChecker {

//         // By default, this method returns true, indicating that extension match
//         // alone is sufficient to determine compatibility. If this method returns
//         // false, then the isFormat(RandomAccessInputStream) method will need to
//         // be checked.
// //			@Override
// //			public boolean suffixSufficient() {
// //				return false;
// //			}

//         // If suffixSufficient returns true, this method has no meaning. Otherwise
//         // if this method returns true (the default) then the extension will have
//         // to match in addition to the result of isFormat(RandomAccessInputStream)
//         // If this returns false, then isFormat(RandomAccessInputStream) is solely
//         // responsible for determining compatibility.
// //			@Override
// //			public boolean suffixNecessary() {
// //				return false;
// //			}

//         // By default, this method returns false and is not considered during
//         // extension checking. If your format uses a magic string, etc... then
//         // you should override this method and check for the string or value as
//         // appropriate.
// //			@Override
// //			public boolean isFormat(final RandomAccessInputStream stream)
// //				throws IOException
// //			{
// //				return stream.readBoolean() == true;
// //			}
//     }

//     // The Reader component uses parsed Metadata to determine how to extract
//     // pixel data from an image source.
//     // In the core SCIFIO library, image planes can be returned as byte[] or
//     // BufferedImages, based on which Reader class is extended. Note that the
//     // BufferedImageReader converts BufferedImages to byte[], so the
//     // ByteArrayReader is typically faster and the default choice here. But
//     // select the class that makes the most sense for your format.
//     public static class Reader extends ByteArrayReader<Metadata> {

//         // The purpose of this method is to populate the provided Plane object by
//         // reading from the specified image and plane indices in the underlying
//         // image source.
//         // planeMin and planeMax are dimensional indices determining the requested
//         // subregion offsets into the specified plane.
//         @Override
//         public ByteArrayPlane openPlane(int imageIndex, long planeIndex,
//                                         ByteArrayPlane plane, long[] planeMin, long[] planeMax,
//                                         SCIFIOConfig config) throws FormatException, IOException
// 			{
// 				// The attached metadata should give us everything we need to determine
// 				// how the provided plane's pixels will be populated.
// 				final Metadata meta = getMetadata();

// 				// For the Reader, there are currently no explicit SCIFIOConfig options.
// 				// If there are K:V pairs of interest, they can be queried:
// 				config.containsKey("key");

// 				// update the data by reference. Ideally, this limits memory problems
// 				// from rapid Java array construction/destruction.
// 				final byte[] bytes = plane.getData();
// 				Arrays.fill(bytes, 0, bytes.length, (byte) 0);

// 				// If the attached Metadata has color tables, we should attach the
// 				// appropriate table to the returned plane.
// 				// NB: this functionality is planned to be moved to the AbstractReader
// 				//     layer so that it always happens and this boilerplate will become
// 				//     unnecessary.
// 				if (meta.getClass().isAssignableFrom(HasColorTable.class)) {
// 					plane.setColorTable(((HasColorTable) meta).getColorTable(imageIndex,
//                                                                              planeIndex));
// 				}

// 				return plane;
// 			}

//         // You must declare what domains your reader is associated with, based
//         // on the list of constants in io.scif.util.FormatTools.
//         // It is also sufficient to return an empty array here.
//         @Override
//         protected String[] createDomainArray() {
//             return new String[0];
//         }

//     }

//     // *** OPTIONAL COMPONENTS ***

//     // Writers are not implemented for proprietary formats, as doing so
//     // typically violates licensing. However, if your format is open source you
//     // are welcome to implement a writer.
//     public static class Writer extends AbstractWriter<Metadata> {

//         // NB: note that there is no writePlane method that uses a SCIFIOConfig.
//         // The writer configuration comes into play in the setDest methods.
//         // Note that all the default SCIFIOConfig#writer[XXXX] functionality is
//         // handled in the Abstract layer.
//         // But if there is configuration state for the writer you need to access,
//         // you should override this setDest signature (as it is the lowest-level
//         // signature, thus guaranteed to be called). Typically you will still want
//         // a super.setDest call to ensure the standard boilerplate is handled
//         // properly.
// //			@Override
// //			public void setDest(final RandomAccessOutputStream out, final int imageIndex,
// //				final SCIFIOConfig config) throws FormatException, IOException
// //			{
// //				super.setDest(out, imageIndex, config);
// //			}

//         // Writers take a source plane and save it to their attached output stream
//         // The image and plane indices are references to the final output dataset
//         @Override
//         public void writePlane(int imageIndex, long planeIndex, Plane plane,
//                                long[] planeMin, long[] planeMax) throws FormatException, IOException
// 			{
// 				// This Metadata object describes how to write the data out to the
// 				// destination image.
// 				final Metadata meta = getMetadata();

// 				// This stream is the destination image to write to.
// 				final RandomAccessOutputStream stream = getStream();

// 				// The given Plane object is the source plane to write
// 				final byte[] bytes = plane.getBytes();

// 				System.out.println(bytes.length);
// 			}

//         // If your writer supports a compression type, you can declare that here.
//         // Otherwise it is sufficient to return an empty String[]
//         @Override
//         protected String[] makeCompressionTypes() {
//             return new String[0];
//         }
//     }

//     // The purpose of a Translator is similar to that of a Parser: to populate
//     // the format-specific metadata of a Metadata object.
//     // However, while a Parser reads from an image source to perform this
//     // operation, a Translator reads from a Metadata object of another format.
//     //
//     // There are two main reasons when you would want to implement a Translator:
//     // 1) If you implement a Writer, you should also implement a Translator to
//     // describe how io.scif.Metadata should be translated to your Format-
//     // specific metadata. This translator will then be called whenever
//     // SCIFIO writes out your format, and it will be able to handle any
//     // input format type. Essentially this is translating ImageMetadata to
//     // your format-specific metadata.
//     // 2) If you are adding support for a new Metadata schema to SCIFIO, you
//     // will probably want to create Translators to and from your new Metadata
//     // schema and core SCIFIO Metadata classes. The purpose of these
//     // Translators is to more accurately or richly capture metadata
//     // information, without the lossy ImageMetadata intermediate that would
//     // be used by default translators.
//     // This is a more advanced use case but mentioned for completeness. See
//     // https://github.com/scifio/scifio-ome-xml/tree/dec59b4f37461a248cc57b1d38f4ebe2eaa3593e/src/main/java/io/scif/ome/translators
//     // for examples of this case.
//     public static class Translator extends
//                                    AbstractTranslator<io.scif.Metadata, Metadata>
//     {

//         // The source and dest methods are used for finding matching Translators
//         // They require only trivial implementations.

//         @Override
//         public Class<? extends io.scif.Metadata> source() {
//             return io.scif.Metadata.class;
//         }

//         @Override
//         public Class<? extends io.scif.Metadata> dest() {
//             return Metadata.class;
//         }

//         // ** TRANSLATION METHODS **
//         // There are three translation method hooks you can use. It is critical
//         // to understand that the source.getAll() method may return a DIFFERENT
//         // list of ImageMetadata than what is passed to these methods.
//         // This is because the source's ImageMetadata may still be the direct
//         // translation of its format-specific Metadata, but the provided
//         // ImageMetadata may be the result of modification - cropping, zooming,
//         // etc...
//         // So, DO NOT CALL:
//         // - Metadata#get(int)
//         // - Metadata#getAll()
//         // in these methods unless you have a good reason to do so. Use the
//         // ImageMetadata provided.
//         //
//         // There are three hooks you can use in translation:
//         // 1) typedTranslate gives you access to the concrete source and
//         // destination metadata objects, along with the ImageMeatadata.
//         // 2) translateFormatMetadata when you want to use format-specific
//         // metadata from the source (only really applicable in reason #2 above
//         // for creating a Translator)
//         // 3) translateImageMetadata when you want to use the source's
//         // ImageMetadata (which is always the case when making a translator
//         // with a general io.scif.Metadata source)

//         // Not used in the general case
// //			@Override
// //			protected void typedTranslate(final io.scif.Metadata source,
// //				final List<ImageMetadata> imageMetadata, final Metadata dest)
// //			{
// //				super.typedTranslate(source, imageMetadata, dest);
// //			}

//         // Not used in the general case
// //			@Override
// //			protected void translateFormatMetadata(final io.scif.Metadata source,
// //				final Metadata dest)
// //			{
// //			}

//         // Here we use the state in the ImageMetadata to populate format-specific
//         // metadata
//         @Override
//         protected void translateImageMetadata(final List<ImageMetadata> source,
//                                               final Metadata dest)
// 			{
// 				ImageMetadata iMeta = source.get(0);
// 				if (iMeta.isIndexed()) {
// 					dest.setColor("red");
// 				}
// 				else {
// 					dest.setColor("blue");
// 				}
// 			}

}
