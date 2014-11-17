package spim.process.interestpointdetection;

import ij.ImageJ;

import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.RandomAccess;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;

public class IntegralImage3d 
{
	final public static long getIndex( final long x, final long y, final long z, final long w, final long h )
	{
		return (x + w * (y + z * h));
	}
	
	final public static Img< LongType > compute( final Img< FloatType > img )
	{
		final Img< LongType > integralTmp;
		
		if ( img instanceof ArrayImg )
		{
			final ArrayImg< LongType, LongArray > i = ArrayImgs.longs( new long[]{ img.dimension( 0 ) + 1, img.dimension( 1 ) + 1, img.dimension( 2 ) + 1 } );
			computeArray( i, (ArrayImg< FloatType, FloatArray >)img );
			integralTmp = i;
		}
		else
		{
			ImgFactory<LongType> imgFactory = null;
			try { imgFactory = img.factory().imgFactory( new LongType() ); } catch (IncompatibleTypeException e)
			{
				e.printStackTrace();
			}
			integralTmp = imgFactory.create( new long[]{ img.dimension( 0 ) + 1, img.dimension( 1 ) + 1, img.dimension( 2 ) + 1 }, new LongType() );
			compute( integralTmp, img );
		}
		
		return integralTmp;
	}

	final public static void computeIntegralImage( final Img< LongType > integralTmp, final Img< FloatType > img )
	{
		if ( ( integralTmp instanceof ArrayImg ) && ( img instanceof ArrayImg ) )
			computeArray( (ArrayImg< LongType, LongArray >)integralTmp, (ArrayImg< FloatType, FloatArray >)img );
		else
			compute( integralTmp, img );
	}


	final private static void computeArray( final ArrayImg< LongType, LongArray > integralTmp, final ArrayImg< FloatType, FloatArray > img )
	{
		final long[] data = integralTmp.update( null ).getCurrentStorageArray();
		final float[] dataF = img.update( null ).getCurrentStorageArray();

		final long w = integralTmp.dimension( 0 );
		final long h = integralTmp.dimension( 1 );
		final long d = integralTmp.dimension( 2 );

		final long wf = img.dimension( 0 );
		final long hf = img.dimension( 1 );

		final AtomicInteger ai = new AtomicInteger(0);
        final Thread[] threads = SimpleMultiThreading.newThreads(  );
        final int numThreads = threads.length;
        
        //
        // sum over x
		//
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int y = 1; y < h; ++y )
	            			{
	            				int indexIn = (int)getIndex( 0, y - 1, z - 1, wf, hf );
	            				int indexOut = (int)getIndex( 1, y, z, w, h );
	            				
	            				// compute the first pixel
	            				long sum = (int)( dataF[ (int)indexIn ] );
	            				data[ (int)indexOut ] = sum;
	            				
	            				for ( int x = 2; x < w; ++x )
	            				{
	            					++indexIn;
	            					++indexOut;
	
	            					sum += (int)( dataF[ (int)indexIn ] );
		            				data[ (int)indexOut ] = sum;
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over y
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
            		
            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				int index = (int)getIndex( x, 1, z, w, h );
	            				
	            				// init sum on first pixel that is not zero
	            				long sum = data[ index ];
	
	            				for ( int y = 2; y < h; ++y )
	            				{
	            					index += w;
	            					
	            					sum += data[ index ];
	            					data[ index ] = sum;
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over z
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
                	final int inc = (int)( w*h );
                	
            		for ( int y = 1; y < h; ++y )
            		{
            			if ( y % numThreads == myNumber )
            			{
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				int index = (int)getIndex( x, y, 1, w, h );
	
	            				//System.out.println( index + " " + data[ index ] );
	            				
	            				// init sum on first pixel that is not zero
	            				long sum = data[ index ];
	
	            				for ( int z = 2; z < d; ++z )
	            				{
	            					index += inc;

		            				//System.out.println( index + " " + data[ index ] );

	            					sum += data[ index ];
	            					data[ index ] = sum;
	            				}
	            				//System.out.println();
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
	}

	final private static void compute( final Img< LongType > integralTmp, final Img< FloatType > img )
	{		
		final long w = integralTmp.dimension( 0 );
		final long h = integralTmp.dimension( 1 );
		final long d = integralTmp.dimension( 2 );

		final long wf = img.dimension( 0 );
		final long hf = img.dimension( 1 );

		final AtomicInteger ai = new AtomicInteger(0);					
        final Thread[] threads = SimpleMultiThreading.newThreads(  );
        final int numThreads = threads.length;
        
        //
        // sum over x
		//
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();
                	
                	final RandomAccess< LongType > data = integralTmp.randomAccess();
                	final RandomAccess< FloatType > dataF = img.randomAccess();

                	final int[] pos = new int[ 3 ];
                	final int[] posF = new int[ 3 ];
                	
            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
            				posF[ 2 ] = z - 1;
            				pos[ 2 ] = z;
            				
	            			for ( int y = 1; y < h; ++y )
	            			{
	            				posF[ 1 ] = y-1;
	            				posF[ 0 ] = 0;
	            				pos[ 1 ] = y;
	            				pos[ 0 ] = 1;
	            				
	            				dataF.setPosition( posF );
	            				data.setPosition( pos );
	            				
	            				//int indexIn = getIndex( 0, y - 1, z - 1, wf, hf );
	            				//int indexOut = getIndex( 1, y, z, w, h );
	            				
	            				// compute the first pixel
	            				long sum = (int)( dataF.get().get() );
	            				data.get().set( sum );
	            				
	            				for ( int x = 2; x < w; ++x )
	            				{
	            					data.fwd( 0 );
	            					dataF.fwd( 0 ); 
	
	            					sum += (int)( dataF.get().get() );
		            				data.get().set( sum );
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over y
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
                	final RandomAccess< LongType > data = integralTmp.randomAccess();

                	final int[] pos = new int[ 3 ];

            		for ( int z = 1; z < d; ++z )
            		{
            			if ( z % numThreads == myNumber )
            			{
            				pos[ 2 ] = z;
            				
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				pos[ 0 ] = x;
	            				pos[ 1 ] = 1;
	            				
	            				data.setPosition( pos );
	            				//int index = getIndex( x, 1, z, w, h );
	            				
	            				// init sum on first pixel that is not zero
	            				long sum = data.get().get();
	
	            				for ( int y = 2; y < h; ++y )
	            				{
	            					data.fwd( 1 );
	            					
	            					sum += data.get().get();
	            					data.get().set( sum );
	            				}
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
        
        //
        // sum over z
		//
        
        ai.set( 0 );
        
        for (int ithread = 0; ithread < threads.length; ++ithread)
            threads[ithread] = new Thread(new Runnable()
            {
                public void run()
                {
                	// Thread ID
                	final int myNumber = ai.getAndIncrement();

                	//int index = 0;
                	final long inc = w*h;
                	final RandomAccess< LongType > data = integralTmp.randomAccess();

                	final int[] pos = new int[ 3 ];

            		for ( int y = 1; y < h; ++y )
            		{
            			if ( y % numThreads == myNumber )
            			{
            				pos[ 1 ] = y;
            				
	            			for ( int x = 1; x < w; ++x )
	            			{
	            				pos[ 0 ] = x;
	            				pos[ 2 ] = 1;
	            				
	            				data.setPosition( pos );
	            				//int index = getIndex( x, y, 1, w, h );

	            				// init sum on first pixel that is not zero
	            				long sum = data.get().get();
	
	            				for ( int z = 2; z < d; ++z )
	            				{
	            					//index += inc;
	            					data.fwd( 2 );

		            				//System.out.println( index + " " + data[ index ] );

	            					sum += data.get().get();//[ index ];
	            					data.get().set( sum );
	            				}
	            				//System.out.println();
	            			}
            			}
            		}
                }
            });
        
        SimpleMultiThreading.startAndJoin( threads );
	}
}
