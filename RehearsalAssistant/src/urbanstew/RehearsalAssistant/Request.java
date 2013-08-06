package urbanstew.RehearsalAssistant;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class Request
{
	public static void cancellable_confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		new Dialog(context, title, content, confirmation, true);
	}
	public static void confirmation(Context context, String title, String content, DialogInterface.OnClickListener confirmation)
	{
		new Dialog(context, title, content, confirmation, false);
	}
	public static void notification(Context context, String title, String content)
	{
		new Dialog(context, title, content, null, false);
	}
	public static void contribution(final Context context)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle(context.getString(R.string.contribute))
    	.setMessage(context.getString(R.string.thank_using))
    	.setPositiveButton
    	(
    		context.getString(R.string.find_how),
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://urbanstew.org/contribute.html")));
    		    }
    		}
    	)
    	.setNegativeButton
    	(
    		context.getString(R.string.not_now),
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    	
    		    }
    		}
    	);
    			
        dialog.show();
		
	}
	public static void recordWidget(final Context context)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle("Sound Recorder Widget")
    	.setMessage(context.getString(R.string.try_widget))
    	.setPositiveButton
    	(
    		context.getString(R.string.download_it),
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    				try
    				{
    					context.startActivity
    			        (
    			        	new Intent
    			        	(
    			        		Intent.ACTION_VIEW,
    			        		Uri.parse("market://search?q=pname:org.urbanstew.RehearsalAssistant.RecordWidget")
    			        	).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    			        );
    				} catch (ActivityNotFoundException e)
    				{
        		    	Toast.makeText(context, R.string.market_start_error, Toast.LENGTH_LONG).show();
    				}
    		    }
    		}
    	)
    	.setNegativeButton
    	(
    		context.getString(R.string.dont_download),
    		new DialogInterface.OnClickListener()
    		{
    		    public void onClick(DialogInterface dialog, int whichButton)
    		    {
    		    }
    		}
    	);
    			
        dialog.show();
	}
}

class Dialog
{
	public Dialog(Context context, String title, String content, DialogInterface.OnClickListener confirmation, boolean cancellable)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(context)
    	.setTitle(title)
    	.setMessage(content)
    	.setPositiveButton
    	(
    		context.getString(R.string.ok),
    		confirmation
    	);
		if(cancellable)
			dialog.setNegativeButton(context.getString(R.string.cancel), null);
        dialog.show();
	}
}
