package spim.process.fusion.boundingbox;

import ij.gui.DialogListener;
import ij.gui.GenericDialog;

import java.awt.AWTEvent;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.TextEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.TimePoint;
import spim.fiji.plugin.GUIHelper;
import spim.fiji.plugin.fusion.BoundingBox;
import spim.fiji.plugin.fusion.Fusion;
import spim.fiji.spimdata.SpimData2;
import spim.process.fusion.boundingbox.automatic.MinFilterThreshold;
import spim.process.fusion.export.ImgExport;

public class AutomaticBoundingBox extends ManualBoundingBox
{
	public static int defaultTimepointIndex = 0;
	public static int defaultChannelIndex = 0;
	public static int defaultDownsamplingAutomatic = 4;
	public static double defaultBackgroundIntensity = 5;
	public static boolean defaultLoadSequentially = true;

	public AutomaticBoundingBox(
			final SpimData2 spimData,
			final ArrayList<Angle> anglesToProcess,
			final ArrayList<Channel> channelsToProcess,
			final ArrayList<Illumination> illumsToProcess,
			final ArrayList<TimePoint> timepointsToProcess )
	{
		super( spimData, anglesToProcess, channelsToProcess, illumsToProcess, timepointsToProcess );
	}

	@Override
	public boolean queryParameters( final Fusion fusion, final ImgExport imgExport )
	{
		// first get an idea of the maximal bounding box
		final double[] minBB = new double[ 3 ];
		final double[] maxBB = new double[ 3 ];
		
		CompleteBoundingBox.computeMaximalBoundingBox( spimData, anglesToProcess, channelsToProcess, illumsToProcess, timepointsToProcess, minBB, maxBB );
	
		// compute dimensions and update size for this instance
		final long[] dim = new long[ maxBB.length ];
		
		for ( int d = 0; d < dim.length; ++d )
		{
			this.min[ d ] = (int)Math.round( minBB[ d ] );
			this.max[ d ] = (int)Math.round( maxBB[ d ] );
			dim[ d ] = this.max[ d ] - this.min[ d ] + 1;
		}
		
		final GenericDialog gd = new GenericDialog( "Automatically define Bounding Box" );
		
		final String[] timepoints = assembleTimepoints( timepointsToProcess );
		final String[] channels = assembleChannels( channelsToProcess );

		if ( defaultTimepointIndex >= timepoints.length )
			defaultTimepointIndex = 0;

		if ( defaultChannelIndex >= channels.length )
			defaultChannelIndex = 0;
				
		gd.addMessage( "Parameters for automatic segmentation", GUIHelper.largestatusfont );
		
		gd.addChoice( "Timepoint", timepoints, timepoints[ defaultTimepointIndex ] );
		gd.addChoice( "Channel", channels, channels[ defaultChannelIndex ] );
		gd.addSlider( "Background intensity [%]", 1.0, 99.0, defaultBackgroundIntensity );
		gd.addSlider( "Downsampling", 1.0, 10.0, defaultDownsamplingAutomatic );
		gd.addCheckbox( "Load_input_images sequentially", defaultLoadSequentially );
		gd.addMessage( "Image size: ???x???x??? pixels", GUIHelper.smallStatusFont, GUIHelper.good );
		Label l = (Label)gd.getMessage();

		gd.addMessage( "" );
		gd.addMessage( "Parameters for final fusion", GUIHelper.mediumstatusfont );
		
		// add listeners and update values
		addListeners( gd, gd.getNumericFields(), l, dim ).dialogItemChanged( gd, new TextEvent( gd, TextEvent.TEXT_VALUE_CHANGED ) );		
		
		gd.showDialog();
		
		if ( gd.wasCanceled() )
			return false;

		final TimePoint timepoint = timepointsToProcess.get( defaultTimepointIndex = gd.getNextChoiceIndex() );
		final Channel channel = channelsToProcess.get( defaultChannelIndex = gd.getNextChoiceIndex() );
		final double background = defaultBackgroundIntensity = gd.getNextNumber();
		this.downsampling = defaultDownsamplingAutomatic = (int)Math.round( gd.getNextNumber() );
		final boolean loadSequentially = defaultLoadSequentially = gd.getNextBoolean();
		
		// compute approx bounding box
		final MinFilterThreshold automatic = new MinFilterThreshold(
				spimData,
				anglesToProcess,
				illumsToProcess,
				channel,
				timepoint,
				this,
				background,
				loadSequentially );
		
		if ( !automatic.run() )
		{
			return false;
		}
		else
		{
			BoundingBox.minStatic = automatic.getMin();
			BoundingBox.maxStatic = automatic.getMax();
		}
				
		return super.queryParameters( fusion, imgExport );
	}
	
	protected String[] assembleTimepoints( final List< TimePoint > timepoints )
	{
		final String[] t = new String[ timepoints.size() ];
		
		for ( int i = 0; i < timepoints.size(); ++i )
			t[ i ] = timepoints.get( i ).getName();
		
		return t;
	}

	protected String[] assembleChannels( final List< Channel > channels )
	{
		final String[] c = new String[ channels.size() ];
		
		for ( int i = 0; i < channels.size(); ++i )
			c[ i ] = channels.get( i ).getName();
		
		return c;
	}

	protected DialogListener addListeners(
			final GenericDialog gd,
			final Vector<?> tf,
			final Label label,
			final long[] dim )
	{
		final TextField downsample = (TextField)tf.get( 1 );
				
		DialogListener d = new DialogListener()
		{
			int downsampling;
			
			@Override
			public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
			{
				if ( (e instanceof TextEvent) && (e.getID() == TextEvent.TEXT_VALUE_CHANGED) )
				{
					downsampling = Integer.parseInt( downsample.getText() );
					
					final long numPixels = numPixels( dim, downsampling );
					final long megabytes = (numPixels * 4) / (1024*1024);				
					
					label.setText( "Image size for segmentation: " + 
							(dim[ 0 ])/downsampling + " x " + 
							(dim[ 1 ])/downsampling + " x " + 
							(dim[ 2 ])/downsampling + " pixels, " + megabytes + " MB" );
					label.setForeground( GUIHelper.good );
				}
				return true;
			}			
		};
		
		gd.addDialogListener( d );
		
		return d;
	}

	protected static long numPixels( final long[] dim, final int downsampling )
	{
		long numpixels = 1;
		
		for ( int d = 0; d < dim.length; ++d )
			numpixels *= (dim[ d ])/downsampling;
		
		return numpixels;
	}

	@Override
	public AutomaticBoundingBox newInstance(
			final SpimData2 spimData,
			final ArrayList<Angle> anglesToProcess,
			final ArrayList<Channel> channelsToProcess,
			final ArrayList<Illumination> illumsToProcess,
			final ArrayList<TimePoint> timepointsToProcess)
	{
		return new AutomaticBoundingBox( spimData, anglesToProcess, channelsToProcess, illumsToProcess, timepointsToProcess );
	}

	@Override
	public String getDescription()
	{
		return "Estimate automatically (experimental)";
	}
}