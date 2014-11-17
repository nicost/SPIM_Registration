package spim.process.interestpointdetection;

import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;

public class DOM
{
	final public static void computeDifferencOfMean3d( final Img< LongType> integralImg, final Img< FloatType > domImg, final int sx1, final int sy1, final int sz1, final int sx2, final int sy2, final int sz2, final float min, final float max  )
	{
		final float diff = max - min;
		
		final float sumPixels1 = sx1 * sy1 * sz1;
		final float sumPixels2 = sx2 * sy2 * sz2;
		
		final float d1 = sumPixels1 * diff;
		final float d2 = sumPixels2 * diff;
		
		final int sx1Half = sx1 / 2;
		final int sy1Half = sy1 / 2;
		final int sz1Half = sz1 / 2;

		final int sx2Half = sx2 / 2;
		final int sy2Half = sy2 / 2;
		final int sz2Half = sz2 / 2;
		
		final int sxHalfMax = Math.max( sx1Half, sx2Half );
		final int syHalfMax = Math.max( sy1Half, sy2Half );
		final int szHalfMax = Math.max( sz1Half, sz2Half );

		final int w = (int)domImg.dimension( 0 ) - ( Math.max( sx1, sx2 ) / 2 ) * 2;
		final int h = (int)domImg.dimension( 1 ) - ( Math.max( sy1, sy2 ) / 2 ) * 2;
		final int d = (int)domImg.dimension( 2 ) - ( Math.max( sz1, sz2 ) / 2 ) * 2;

		final AtomicInteger ai = new AtomicInteger(0);					
		final Thread[] threads = SimpleMultiThreading.newThreads();
		final int numThreads = threads.length;
		
		for (int ithread = 0; ithread < threads.length; ++ithread)
		threads[ithread] = new Thread(new Runnable()
		{
			public void run()
			{
				// Thread ID
				final int myNumber = ai.getAndIncrement();
		
				// for each computation we need 8 randomaccesses, so 16 all together
				final RandomAccess< LongType > r11 = integralImg.randomAccess();
				final RandomAccess< LongType > r12 = integralImg.randomAccess();
				final RandomAccess< LongType > r13 = integralImg.randomAccess();
				final RandomAccess< LongType > r14 = integralImg.randomAccess();
				final RandomAccess< LongType > r15 = integralImg.randomAccess();
				final RandomAccess< LongType > r16 = integralImg.randomAccess();
				final RandomAccess< LongType > r17 = integralImg.randomAccess();
				final RandomAccess< LongType > r18 = integralImg.randomAccess();
		
				final RandomAccess< LongType > r21 = integralImg.randomAccess();
				final RandomAccess< LongType > r22 = integralImg.randomAccess();
				final RandomAccess< LongType > r23 = integralImg.randomAccess();
				final RandomAccess< LongType > r24 = integralImg.randomAccess();
				final RandomAccess< LongType > r25 = integralImg.randomAccess();
				final RandomAccess< LongType > r26 = integralImg.randomAccess();
				final RandomAccess< LongType > r27 = integralImg.randomAccess();
				final RandomAccess< LongType > r28 = integralImg.randomAccess();
				
				final RandomAccess< FloatType > result = domImg.randomAccess();
				
				final int[] p = new int[ 3 ];
		
				for ( int z = 0; z < d; ++z )
				{
					if ( z % numThreads == myNumber )
					{
						for ( int y = 0; y < h; ++y )
						{
							// set the result randomaccess
							p[ 0 ] = sxHalfMax; p[ 1 ] = y + syHalfMax; p[ 2 ] = z + szHalfMax;
							result.setPosition( p );
							
							// set all randomaccess for the first box accordingly
							p[ 0 ] = sxHalfMax - sx1Half; p[ 1 ] = y + syHalfMax - sy1Half; p[ 2 ] = z + szHalfMax - sz1Half;
							r11.setPosition( p ); // negative
			
							p[ 0 ] += sx1;
							r12.setPosition( p ); // positive
							
							p[ 1 ] += sy1;
							r13.setPosition( p ); // negative
							
							p[ 0 ] -= sx1;
							r14.setPosition( p ); // positive
			
							p[ 2 ] += sz1;
							r15.setPosition( p ); // negative
			
							p[ 0 ] += sx1;
							r16.setPosition( p ); // positive
			
							p[ 1 ] -= sy1;
							r17.setPosition( p ); // negative
			
							p[ 0 ] -= sx1;
							r18.setPosition( p ); // positive
			
							// set all randomaccess for the second box accordingly
							p[ 0 ] = sxHalfMax - sx2Half; p[ 1 ] = y + syHalfMax - sy2Half; p[ 2 ] = z + szHalfMax - sz2Half;
							r21.setPosition( p );
			
							p[ 0 ] += sx2;
							r22.setPosition( p ); // positive
							
							p[ 1 ] += sy2;
							r23.setPosition( p ); // negative
							
							p[ 0 ] -= sx2;
							r24.setPosition( p ); // positive
			
							p[ 2 ] += sz2;
							r25.setPosition( p ); // negative
			
							p[ 0 ] += sx2;
							r26.setPosition( p ); // positive
			
							p[ 1 ] -= sy2;
							r27.setPosition( p ); // negative
			
							p[ 0 ] -= sx2;
							r28.setPosition( p ); // positive
			
							for ( int x = 0; x < w; ++x )
							{
								final long s1 = -r11.get().get() + r12.get().get() - r13.get().get() + r14.get().get() - r15.get().get() + r16.get().get() - r17.get().get() + r18.get().get();
								final long s2 = -r21.get().get() + r22.get().get() - r23.get().get() + r24.get().get() - r25.get().get() + r26.get().get() - r27.get().get() + r28.get().get();
			
								result.get().set( (float)s2/d2 - (float)s1/d1 );
								
								if ( x != w - 1 )
								{
									r11.fwd( 0 ); r12.fwd( 0 ); r13.fwd( 0 ); r14.fwd( 0 ); r15.fwd( 0 ); r16.fwd( 0 ); r17.fwd( 0 ); r18.fwd( 0 );
									r21.fwd( 0 ); r22.fwd( 0 ); r23.fwd( 0 ); r24.fwd( 0 ); r25.fwd( 0 ); r26.fwd( 0 ); r27.fwd( 0 ); r28.fwd( 0 );
									result.fwd( 0 );
								}
							}
						}
						}
					}
				}
			});
		
		SimpleMultiThreading.startAndJoin( threads );
	}

	/**
	 * Compute the average in the area
	 * 
	 * @param fromX - start coordinate in x (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromY - start coordinate in y (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param fromZ - start coordinate in z (exclusive in integral image coordinates, inclusive in image coordinates)
	 * @param sX - number of pixels in x
	 * @param sY - number of pixels in y
	 * @param sZ - number of pixels in z
	 * @param randomAccess - randomAccess on the integral image
	 * @return
	 */
	final public static long computeSum( final int fromX, final int fromY, final int fromZ, final int vX, final int vY, final int vZ, final RandomAccess< LongType > randomAccess )
	{
		randomAccess.setPosition( fromX, 0 );
		randomAccess.setPosition( fromY, 1 );
		randomAccess.setPosition( fromZ, 2 );
		
		long sum = -randomAccess.get().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.get().get();
		
		randomAccess.move( vY, 1 );
		sum += -randomAccess.get().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.get().get();
		
		randomAccess.move( vZ, 2 );
		sum += -randomAccess.get().get();
		
		randomAccess.move( vX, 0 );
		sum += randomAccess.get().get();
		
		randomAccess.move( -vY, 1 );
		sum += -randomAccess.get().get();
		
		randomAccess.move( -vX, 0 );
		sum += randomAccess.get().get();
		
		return sum;
	}
}
