package mg.mgmap.generic.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.map.model.DisplayModel;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.util.CC;

public class VUtil {

    public static final float QC_HEIGHT_DP = 48;
    public static final int QC_HEIGHT = dp( QC_HEIGHT_DP);
    public static int dp(float f){
        return (int) (f * DisplayModel.getDeviceScaleFactor());
    }

    public final static int hMargin  = dp(1.5f);
    public final static int vMargin = dp(2);

    /**
     * Create QuickControl
     * @param parent parent of QuickControl
     * @param linearLayoutParent true if parent has LinearLayout - false if parent has RelativeLayout
     * @return created QuickControl view
     */
    public static ExtendedTextView createQuickControlETV(ViewGroup parent, boolean linearLayoutParent /* else relativeLayoutParent*/) {
        Context context = parent.getContext();
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(36));
        parent.addView(etv);

        if (linearLayoutParent){
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            params.setMargins(hMargin,vMargin,hMargin,vMargin);
            params.weight = 20;
            etv.setLayoutParams(params);
        } else {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
            params.setMargins(hMargin,vMargin,hMargin,vMargin);
            etv.setLayoutParams(params);
        }

        etv.setPadding(0, dp(4),0, dp(4));
        etv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        etv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((left != oldLeft) || (top != oldTop) || (right != oldRight) || (bottom != oldBottom)){
                int paddingHorizontal = Math.max((right-left - etv.getDrawableSize()) / 2, 0);
                etv.setPadding(paddingHorizontal,etv.getPaddingTop(),paddingHorizontal,etv.getPaddingBottom());
            }
        });
        return etv;
    }

    public static ExtendedTextView createControlETV(ViewGroup parent) {
        Context context = parent.getContext();
        ExtendedTextView etv = new ExtendedTextView(context);
        parent.addView(etv, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = dp(2f);
        params.setMargins(margin,margin,margin,margin);
        etv.setLayoutParams(params);
        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        etv.setTextColor(CC.getColor(R.color.WHITE));

        etv.setPadding(dp(4), dp(4),dp(4), dp(4));
        etv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        return etv;
    }


    public static RelativeLayout createQCRow(Context context){
        RelativeLayout qcRow = new RelativeLayout(context);
        qcRow.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return qcRow;
    }

    public static int getX4QC(int totalWidth, int idx, int max){ // idx from 0.. max-1
        float widthPerQC = totalWidth/(float)max;
        return (int)(idx*widthPerQC);
    }


}