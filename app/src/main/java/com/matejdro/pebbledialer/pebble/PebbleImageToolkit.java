package com.matejdro.pebbledialer.pebble;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.pixels.PixelsWriter;


public class PebbleImageToolkit
{
    public static int[] PEBBLE_TIME_PALETTE = new int[64];
    public static HashMap<Integer, Byte> PEBBLE_TIME_PALETTE_MAP = new HashMap<>();
    static
    {
        int counter = 0;

        for (int r = 0x000000; r <= 0xFF0000; r += 0x550000)
        {
            for (int g = 0x000000; g <= 0x00FF00; g += 0x005500)
            {
                for (int b = 0x000000; b <= 0x0000FF; b += 0x000055)
                {
                    int color = r | g | b;
                    PEBBLE_TIME_PALETTE[counter] = color;
                    PEBBLE_TIME_PALETTE_MAP.put(color, (byte) counter);

                    counter++;
                }
            }
        }
    }

   public static Bitmap resizePreservingRatio(Bitmap original, int newWidth, int newHeight)
   {
       int originalWidth = original.getWidth();
       int originalHeight = original.getHeight();

       if (originalWidth * newWidth < originalHeight * newHeight)
       {
           newHeight = originalHeight * newWidth / originalWidth;
       }
       else
       {
           newWidth = originalWidth * newHeight / originalHeight;
       }

       return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
   }

    public static void ditherToPebbleTimeColors(Bitmap bitmap)
    {
        SeparatedColor[][] separatedColorArray = new SeparatedColor[bitmap.getWidth()][bitmap.getHeight()];

        for (int y = 0; y < bitmap.getHeight(); y++)
        {
            for (int x = 0; x < bitmap.getWidth(); x++)
            {
                separatedColorArray[x][y] = new SeparatedColor(bitmap.getPixel(x, y));
            }
        }

        for (int y = 0; y < bitmap.getHeight(); y++)
        {
            for (int x = 0; x < bitmap.getWidth(); x++)
            {
                SeparatedColor oldColor = separatedColorArray[x][y];
                SeparatedColor newColor = oldColor.getNearestPebbleTimeColor();
                bitmap.setPixel(x, y, newColor.toRGB());

                SeparatedColor quantError = oldColor.sub(newColor);

                if (x < bitmap.getWidth() - 1)
                    separatedColorArray[x + 1][y] = separatedColorArray[x + 1][y].add(quantError.multiply(7d / 16));

                if (y < bitmap.getHeight() - 1)
                {
                    if (x > 0)
                        separatedColorArray[x - 1][y + 1] = separatedColorArray[x - 1][y + 1].add(quantError.multiply(3d / 16));

                    separatedColorArray[x][y + 1] = separatedColorArray[x][y + 1].add(quantError.multiply(5d / 16));

                    if (x < bitmap.getWidth() - 1)
                        separatedColorArray[x + 1][y + 1] = separatedColorArray[x + 1][y + 1].add(quantError.multiply(1d / 16));
                }

            }
        }
    }

    public static void writeIndexedPebblePNG(Bitmap bitmap, OutputStream stream)
    {
        ImageInfo imageInfo = new ImageInfo(bitmap.getWidth(), bitmap.getHeight(), 8, false, false, true);
        PngWriter pngWriter = new PngWriter(stream, imageInfo);

        PngChunkPLTE paletteChunk = pngWriter.getMetadata().createPLTEChunk();
        paletteChunk.setNentries(64);
        for (int i = 0; i < 64; i++)
        {
            int color = PEBBLE_TIME_PALETTE[i];
            paletteChunk.setEntry(i, Color.red(color), Color.green(color), Color.blue(color));
        }

        for (int y = 0; y < bitmap.getHeight(); y++)
        {
            ImageLineByte imageLine = new ImageLineByte(imageInfo);
            for (int x = 0; x < bitmap.getWidth(); x++)
            {
                int pixel = bitmap.getPixel(x, y) & 0x00FFFFFF;
                Byte index = PEBBLE_TIME_PALETTE_MAP.get(pixel);

                if (index == null)
                    throw new IllegalArgumentException("Color is not supported by Pebble Time: " + Integer.toHexString(pixel));

                imageLine.getScanline()[x] = index;
            }

            pngWriter.writeRow(imageLine, y);
        }

        pngWriter.end();
    }

    public static byte[] getIndexedPebbleImageBytes(Bitmap bitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeIndexedPebblePNG(bitmap, outputStream);
        return outputStream.toByteArray();

    }

    public static class SeparatedColor
    {
        private int r;
        private int g;
        private int b;

        public SeparatedColor(int rgb)
        {
            this.r = Color.red(rgb);
            this.g = Color.green(rgb);
            this.b = Color.blue(rgb);
        }

        public SeparatedColor(int r, int g, int b)
        {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public SeparatedColor add(SeparatedColor other)
        {
            return new SeparatedColor(r + other.r, g + other.g, b + other.b);
        }

        public SeparatedColor sub(SeparatedColor other)
        {
            return new SeparatedColor(r - other.r, g - other.g, b - other.b);
        }

        public SeparatedColor multiply(double scalar)
        {
            return new SeparatedColor((int) (r * scalar), (int) (g * scalar), (int) (b * scalar));
        }

        public SeparatedColor getNearestPebbleTimeColor()
        {
            return new SeparatedColor(Math.round(r / 85f) * 85, Math.round(g / 85f) * 85, Math.round(b / 85f) * 85);
        }

        public int toRGB()
        {
            return Color.rgb(r, g, b);
        }
    }
}
