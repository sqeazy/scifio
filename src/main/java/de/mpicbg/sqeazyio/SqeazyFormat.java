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
import java.nio.ByteBuffer;

import net.imagej.axis.Axes;
import net.imglib2.Interval;

import org.bridj.Pointer;
import org.bridj.CLong;

import static org.bridj.Pointer.*;

import sqeazy.bindings.SqeazyLibrary;

import org.scijava.plugin.Plugin;
import org.scijava.util.Bytes;

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
@Plugin(type = Format.class, name = "Sqeazy")
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
		 * Because we have no way of indexing into the sqy file efficiently in
		 * general, we cheat and store the entire file's data in a giant array.
		 */
	    private byte[] bytes;

		// /** Current row number. */
		// private int row;

		// /** Number of tokens per row. */
		// private int rowLength;

		// /** Column index for X coordinate. */
		// private int xIndex = -1;

		// /** Column index for Y coordinate. */
		// private int yIndex = -1;


		/** Image width. */
        @Field(label = "width")
		private int sizeX = 0;

		/** Image height. */
		@Field(label = "heigth")
		private int sizeY = 0;

        /** Image width. */
		@Field(label = "depth")
		private int sizeZ = 0;

        /** threads to use. */
		@Field(label = "nthreads")
		private int nThreads = 1;


		// -- TextMetadata getters and setters --

		public byte[] getData() {
			return bytes;
		}

		public void setData(final byte[] data) {
			this.bytes = data;
		}


		public int getNthreads() {
			return nThreads;
		}

		public void setNthreads(final int nthreads) {
			this.nThreads = nthreads;
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
				bytes = null;
				// rowLength = 0;
				// xIndex = yIndex = -1;
				// channels = null;
				sizeX = sizeY = sizeZ = 0;
				// row = 0;
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

            final Pointer<Byte> bHdr = pointerToCString(data);
            final Pointer<CLong> lLength = Pointer.allocateCLong().setLong(data.length());
            final int iRValue = SqeazyLibrary.SQY_Header_Size(bHdr,lLength);

            if(lLength.getInt() != 0 && iRValue == 0){
                return true;
            } else {
                System.out.println("[SqeazyFormat.java] SqeazyLibrary.SQY_Header_Size called");
                System.out.println("detected size "+lLength.getInt()+", return value "+iRValue+"\n");
                System.out.println(data.substring(0,50)+"\n");
            }

			return false;
		}
	}
    // The Parser is your interface with the image source.
    // It has one purpose: to take the raw image information and generate a
    // Metadata instance, populating all format-specific fields.
    public static class Parser extends AbstractParser<Metadata> {


        // In this method we populate the given Metadata object
        @Override
        public void typedParse(final RandomAccessInputStream stream,
                               final Metadata meta,
                               final SCIFIOConfig config) throws IOException, FormatException
			{
                meta.createImageMetadata(1);
                final ImageMetadata iMeta = meta.get(0);

                // HEADER
                // read file partially into memory to extract header
                log().info("Reading sqy file "+stream.getFileName());
                int hdr_size = 4 << 10;
                if(stream.length() < hdr_size)
                    hdr_size = (int)stream.length();

                if (!FormatTools.validStream(stream, hdr_size , false)){
                    log().error("unable to read sqeazy header of size "+hdr_size);
                    return ;
                }
                final long bytes = stream.length();

                log().info("Parsing file header");
                final ByteBuffer blob = ByteBuffer.allocate(4 << 10);
                stream.seek(0);
                stream.read(blob, 0, hdr_size);//read 4MB

                final Pointer<Byte> bHdr = pointerToBytes(blob);
                final Pointer<CLong> lLength = Pointer.allocateCLong().setCLong(4 << 10);
                int sqy_status = SqeazyLibrary.SQY_Header_Size(bHdr,lLength);
                if(lLength.getInt() == 0 && sqy_status != 0){
                    log().error("unable to read sqeazy header");
                    return;
                }

                final ByteBuffer header = ByteBuffer.allocate((int)lLength.getCLong());
                stream.seek(0);
                stream.read(header, 0, (int)lLength.getCLong());

                Pointer<Byte> bSQYHeader = pointerToBytes(header);
                int sizeZ = 0;
                final int sizeC = 1;
                final int sizeT = 1;

                sqy_status = SqeazyLibrary.SQY_Decompressed_Sizeof(bSQYHeader,lLength);
                if(sqy_status != 0){
                    log().error("unable to read Sizeof pixel from sqeazy header");
                    return;
                }
                final int sizeof = (int)lLength.getCLong();
                if(sizeof == 2){
                    iMeta.setPixelType(FormatTools.UINT16);
                }
                else{
                    iMeta.setPixelType(FormatTools.UINT8);
                }

                iMeta.setLittleEndian(false);

                lLength.setCLong((long)header.capacity());
                sqy_status = SqeazyLibrary.SQY_Decompressed_NDims(bSQYHeader,lLength);
                if(sqy_status != 0){
                    log().error("unable to read NDims of volume from sqeazy header");
                    return;
                }

                final int ndims = (int)lLength.getCLong();
                iMeta.setPlanarAxisCount(ndims);

                final Pointer<CLong> lShape = Pointer.allocateCLongs(lLength.getCLong());
                lShape.setCLongAtIndex(0,(long)header.capacity());

                sqy_status = SqeazyLibrary.SQY_Decompressed_Shape(bSQYHeader,lShape);
                if(sqy_status != 0){
                    log().error("unable to read Shape of volume from sqeazy header");
                    return;
                }

                log().info("parsed shape: "+lShape.getCLongAtIndex(0)+" "+lShape.getCLongAtIndex(1)+" "+lShape.getCLongAtIndex(2)+" sizeof="+sizeof);
                iMeta.setAxisLength(Axes.X, lShape.getCLongAtIndex(ndims-1));
                meta.setSizeX((int)lShape.getCLongAtIndex(ndims-1));

                iMeta.setAxisLength(Axes.Y, lShape.getCLongAtIndex(ndims-2));
                meta.setSizeY((int)lShape.getCLongAtIndex(ndims-2));

                if(iMeta.getPlanarAxisCount() == 3){
                    iMeta.setAxisLength(Axes.Z, lShape.getCLongAtIndex(ndims-3));
                    meta.setSizeZ((int)lShape.getCLongAtIndex(ndims-3));
                    sizeZ = (int)meta.getSizeZ();
                }
                iMeta.setAxisLength(Axes.CHANNEL, 1);
                iMeta.setAxisLength(Axes.TIME, 1);

                final int planeCount = sizeZ * sizeC * sizeT;
                final int planeSize = (int) iMeta.getAxisLength(Axes.X) * (int) iMeta.getAxisLength(Axes.Y);
                final long nbytes = planeCount*planeSize*sizeof;
                final Pointer<Byte> lDecodedBytes = Pointer.allocateBytes(nbytes);
                log().info("allocating ByteBuffer of "+nbytes+" Bytes");

                stream.seek(0);
                final ByteBuffer encoded = ByteBuffer.allocate((int)bytes);
                stream.read(encoded);//read all

                final Pointer<Byte> lCompressedBytes = pointerToBytes(encoded);
                final Pointer<CLong> lCompresssedBufferLength = Pointer.allocateCLong().setCLong(bytes);

                int return_code = -1;
                if(sizeof == 1){
                    log().info("Decompressing 8-bit volume");
                    return_code = SqeazyLibrary.SQY_Decode_UI8(lCompressedBytes,
                                                               bytes,//lCompresssedBufferLength,
                                                 lDecodedBytes,
                                                 meta.getNthreads());
                }
                else if(sizeof == 2){
                    log().info("Decompressing 16-bit volume");
                    return_code = SqeazyLibrary.SQY_Decode_UI16(lCompressedBytes,
                                                                bytes,//lCompresssedBufferLength,
                                                  lDecodedBytes,
                                                  meta.getNthreads());

                }
                else{
                    log().error("unable to decompress of unknown pixel size "+sizeof+" (only sizeof={1 or 2} supported)");
                    return;
                }

                if(return_code == 0){
                    log().info("Decompression successful");
                }

                meta.setData(lDecodedBytes.getBytes((int)nbytes));
			}
    }

    // The Reader component uses parsed Metadata to determine how to extract
    // pixel data from an image source.
    // In the core SCIFIO library, image planes can be returned as byte[] or
    // BufferedImages, based on which Reader class is extended. Note that the
    // BufferedImageReader converts BufferedImages to byte[], so the
    // ByteArrayReader is typically faster and the default choice here. But
    // select the class that makes the most sense for your format.
    public static class Reader extends ByteArrayReader<Metadata> {

        // The purpose of this method is to populate the provided Plane object by
        // reading from the specified image and plane indices in the underlying
        // image source.
        // planeMin and planeMax are dimensional indices determining the requested
        // subregion offsets into the specified plane.
        @Override
        public ByteArrayPlane openPlane(final int imageIndex,
                                        final long planeIndex,
                                        final ByteArrayPlane plane,
                                        final Interval bounds,// final long[] planeMin,
                                        // final long[] planeMax,
                                        final SCIFIOConfig config) throws FormatException, IOException
			{
				// The attached metadata should give us everything we need to determine
				// how the provided plane's pixels will be populated.
				final Metadata meta = getMetadata();

                // update the data by reference. Ideally, this limits memory problems
				// from rapid Java array construction/destruction.
				final byte[] bytes = plane.getData();
				//Arrays.fill(bytes, 0, bytes.length, (byte) 0);
                // FormatTools.checkPlaneForReading(meta,
                //                                  imageIndex,
                //                                  planeIndex,
                //                                  bytes.length,
                //                                  bounds);
                // final int xAxis = meta.get(imageIndex).getAxisIndex(Axes.X);
                // final int yAxis = meta.get(imageIndex).getAxisIndex(Axes.Y);
                final int w = meta.getSizeX(), h = meta.getSizeY(), size = w*h;

                final long planeOffset = planeIndex*w*h;
                final long planeEnd = (long)(planeOffset+size);
                // copy floating point data into byte buffer

                for (long i = planeOffset; i < planeEnd; i++) {
                    bytes[(int)(i - planeOffset)] = meta.getData()[(int)i];
                }

				return plane;
			}

        // You must declare what domains your reader is associated with, based
        // on the list of constants in io.scif.util.FormatTools.
        // It is also sufficient to return an empty array here.
        @Override
        protected String[] createDomainArray() {
            return new String[] { FormatTools.UNKNOWN_DOMAIN };
        }

    }

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
