package com.jonglen7.jugglinglab.ui;

import greendroid.widget.QuickActionGrid;
import greendroid.widget.QuickActionWidget;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;

import com.jonglen7.jugglinglab.R;
import com.jonglen7.jugglinglab.jugglinglab.core.PatternRecord;
import com.jonglen7.jugglinglab.util.Collection;
import com.jonglen7.jugglinglab.util.DataBaseHelper;
import com.jonglen7.jugglinglab.util.Trick;

public class QuickActionGridTrick extends QuickActionGrid {

	final static int STAR = 0;
	final static int EDIT = 1;
	final static int LIST = 2;
	final static int SHARE = 3;
	final static int DELETE = 4;

	Context context;
	PatternRecord pattern_record;
	Intent intent;
	Activity activity;

	public QuickActionGridTrick(Context context) {
		super(context);
		this.context = context;
        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_star, R.string.gd_star));
        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_edit, R.string.gd_edit));
        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_list, R.string.quickactions_trick_collections));
        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_share, R.string.gd_share));
        //TODO Romain (stats): Uncomment when ready (Catches, Stats), cf icons http://androiddrawableexplorer.appspot.com/
//        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_add, R.string.quickactions_trick_catches));
//        this.addQuickAction(new MyQuickAction(context, R.drawable.gd_action_bar_info, R.string.quickactions_trick_stats));
        this.addQuickAction(new MyQuickAction(context, android.R.drawable.ic_delete, R.string.quickactions_delete));

        this.setOnQuickActionClickListener(mActionListener);
	}

    private OnQuickActionClickListener mActionListener = new OnQuickActionClickListener() {
        public void onQuickActionClicked(QuickActionWidget widget, int position) {
    		final Trick trick = new Trick(pattern_record, context);
    		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    		
        	switch (position) {
	    	case STAR:
	    		trick.star();
        		activity.finish();
        		activity.startActivity(intent);
	    		break;
	    		
        	case EDIT:
        		final EditText input = new EditText(context);
        		builder.setView(input);
        		builder.setTitle(context.getString(R.string.gd_edit));
        		input.setText(trick.getCUSTOM_DISPLAY());
        		
        		builder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int whichButton) {
		        		trick.edit(input.getText().toString());
		        		// For the animation, since it gets the display from the pattern_record,
		        		// that is put in the Intent, we need to update it too
		        		pattern_record.setDisplay(input.getText().toString());
		        		activity.finish();
		        		activity.startActivity(intent);
	        		}
        		});

        		builder.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int whichButton) {
	        			dialog.cancel();
	        		}
        		});
        		builder.show();
        		break;
        		
        	case LIST:
        		final ArrayList<Collection> collections = createCollectionList();
        		ArrayList<String> collections_displays = new ArrayList<String>();
        		for (Collection c: collections) collections_displays.add(c.getCUSTOM_DISPLAY());
        		String[] collections_display = (String[])collections_displays.toArray(new String[0]);
        		
        		boolean[] checkedItems = new boolean[collections.size()];
        		ArrayList<Collection> trick_collections = trick.getCollections();
        		for (int i = 0; i < collections.size(); i++) {
        			if (collections.get(i).indexOf(trick_collections) >= 0) checkedItems[i] = true;
        			else checkedItems[i] = false;
        		}
        		
        		builder.setMultiChoiceItems(collections_display, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        		        trick.updateCollection(collections.get(which));
					}
        		});
        		
        		builder.setTitle(context.getString(R.string.quickactions_trick_collections));
        		builder.show();
        		break;
        		
        	case SHARE:
        		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        		shareIntent.setType("text/plain");
        		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
        				context.getString(R.string.quickactions_trick_share_training, pattern_record.getDisplay()) + "\n" +
        				"http://jugglinglab.sourceforge.net/siteswap.php?" + pattern_record.getAnim());
        		context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.quickactions_trick_share)));
        		break;
        		
//        	case 4: // TODO Romain (stats): Catches
//        		Toast.makeText(context, "Catches (popup)", Toast.LENGTH_LONG).show();
//        		break;
//        		
//        	case 5: // TODO Romain (stats): Stats
//        		Toast.makeText(context, "Stats (activity)", Toast.LENGTH_LONG).show();
//        		break;
        		
        	case DELETE:
        		builder.setTitle(R.string.alert_dialog_two_buttons_title);
        		
        		builder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int whichButton) {
	            		trick.delete();
		        		activity.finish();
		        		activity.startActivity(intent);
	        		}
        		});

        		builder.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
	        		public void onClick(DialogInterface dialog, int whichButton) {
	        			dialog.cancel();
	        		}
        		});
        		builder.show();
        		break;
        		
        	default:
        		break;
        	}
        }
    };
    
    public void show(View view, PatternRecord pattern_record, Intent intent, Activity activity) {
    	this.pattern_record = pattern_record;
    	this.intent = intent;
    	this.activity = activity;
    	super.show(view);
    }
    
    private ArrayList<Collection> createCollectionList() {
    	DataBaseHelper myDbHelper = DataBaseHelper.init(context);
        
    	ArrayList<Collection> collections = new ArrayList<Collection>();
		
	 	String query = "SELECT DISTINCT ID_COLLECTION, IS_TUTORIAL, XML_LINE_NUMBER, CUSTOM_DISPLAY " +
	 					"FROM Collection " +
	 					"ORDER BY ID_COLLECTION";
    	Cursor cursor = myDbHelper.execQuery(query);
    	
	 	cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
        	Collection c = new Collection(cursor, context);
        	if (!c.isStarred()) collections.add(c);
            cursor.moveToNext();
        }

	 	cursor.close();

        myDbHelper.close();
    	
		return collections;
	}

}
