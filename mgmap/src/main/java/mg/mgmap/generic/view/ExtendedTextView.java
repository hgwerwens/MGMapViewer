/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.util.Observable;
import java.util.Observer;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.NameUtil;

public class ExtendedTextView extends AppCompatTextView {

    private Pref<Boolean> prState1=null, prState2=null;
    private Pref<Boolean> prAction1=null, prAction2=null;
    private Pref<Boolean> prEnabled=null;

    private int drId1=0,drId2=0,drId3=0,drId4=0;
    private int drIdDis=0;

    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;
    private int drawableSize = 0;
    private String help = "";
    private String help1 = null, help2 = null, help3 = null, help4 = null;
    private String logName = "";

    private final Observer prefObserver = createObserver();

    public ExtendedTextView(Context context) {
        this(context, null);
    }

    public ExtendedTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedTextView setName(String logName){
        this.logName = logName;
        return this;
    }
    public ExtendedTextView setData(int drId){
        drId1 = drId;
        onChange(null);
        return this;
    }
    public ExtendedTextView setData(Pref<Boolean> prState1, int drId1, int drId2){
        this.prState1 = prState1;
        prState1.addObserver(prefObserver);
        this.drId1 = drId1;
        this.drId2 = drId2;
        onChange(null);
        return this;
    }
    public ExtendedTextView setData(Pref<Boolean> prState1, Pref<Boolean> prState2, int drId1, int drId2, int drId3, int drId4){
        this.prState1 = prState1;
        prState1.addObserver(prefObserver);
        this.prState2 = prState2;
        prState2.addObserver(prefObserver);
        this.drId1 = drId1;
        this.drId2 = drId2;
        this.drId3 = drId3;
        this.drId4 = drId4;
        onChange(null);
        return this;
    }
    public ExtendedTextView setPrAction(Pref<Boolean> prAction){
        this.prAction1 = prAction;
        this.setOnClickListener(prAction);
        return this;
    }
    public ExtendedTextView setPrAction(Pref<Boolean> prAction1, Pref<Boolean> prAction2){
        this.prAction1 = prAction1;
        this.setOnClickListener(prAction1);
        this.prAction2 = prAction2;
        this.setOnLongClickListener(prAction2);
        return this;
    }
    public ExtendedTextView setDisabledData(Pref<Boolean> prEnabled, int drIdDis){
        this.prEnabled = prEnabled;
        prEnabled.addObserver(prefObserver);
        this.drIdDis = drIdDis;
        setEnabled();
        onChange("setDisabledData");
        return this;
    }
    public ExtendedTextView setHelp(String help){
        this.help = help;
        return this;
    }
    public ExtendedTextView setHelp(String help1,String help2){
        this.help1 = help1;
        this.help2 = help2;
        return this;
    }
    public ExtendedTextView setHelp(String help1,String help2,String help3,String help4){
        this.help1 = help1;
        this.help2 = help2;
        this.help3 = help4;
        this.help4 = help3;
        return this;
    }
    public String getHelp(){
        String line2 = "";
        if ((prEnabled!=null) && (!prEnabled.getValue())){
            line2 = System.lineSeparator()+"disabled";
        } else {
//            if (prEnabled==null){
//                line2 = "";
//            } else {
//                line2 = "enabled";
//            }
            String res = null;
            if ((prState1 != null) && (prState2 == null)){
                res = prState1.getValue()?help2:help1;
            }
            if ((prState1 != null) && (prState2 != null)){
                if (prState2.getValue()){
                    res = prState1.getValue()?help4:help3;
                } else {
                    res = prState1.getValue()?help2:help1;
                }
            }
            if ((res!=null) && (res.length() > 0)){
//                line2 = ((line2.length() > 0)?"; ":"")  + res;
                line2 = System.lineSeparator()+res;
            }
        }
//        return (help.length()>0)?(help + ((line2.length()>0)?(System.lineSeparator()+line2):"")):"";
        return help + line2;
    }
    public ExtendedTextView addActionObserver(Observer action1Observer){
        if ((prAction1!=null) && (action1Observer!=null)) prAction1.addObserver(action1Observer);
        return this;
    }
    public ExtendedTextView addActionObserver(Observer action1Observer,Observer action2Observer){
        if ((prAction1!=null) && (action1Observer!=null)) prAction1.addObserver(action1Observer);
        if ((prAction2!=null) && (action2Observer!=null)) prAction2.addObserver(action2Observer);
        return this;
    }

    private Observer createObserver(){
        return new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String info;
                if (o instanceof Pref<?>) {
                    Pref<?> pref = (Pref<?>) o;
                    info = "update on "+ pref.getKey();
                } else {
                    info = "update on "+o.toString();
                }
                if (o == prEnabled){
                    setEnabled();
                }
                onChange(info);
            }
        };
    }

    private void setEnabled(){
        setEnabled((prEnabled!=null)?prEnabled.getValue():true);
    }

    public ExtendedTextView setFormat(Formatter.FormatType formatType){
        this.formatType = formatType;
        return this;
    }
    public boolean setValue(Object value){
        String newText = Formatter.format(formatType, value);
        if (!newText.equals(getText())){
            setText( newText );
            onChange("onSetValue: "+newText);
            return true;
        }
        return false;
    }

    public int getDrawableSize(){
        return drawableSize;
    }
    public ExtendedTextView setDrawableSize(int drawableSize){
        this.drawableSize = drawableSize;
        return this;
    }

    private void onChange(String reason){
        if (reason != null){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" n="+logName+" "+((prState1==null)?"":prState1.toString())+" "+((prState2==null)?"":prState2.toString())+" "+reason);
        }
        int drId;
        if ((prEnabled != null) && (!prEnabled.getValue())){
            drId = drIdDis;
        } else { // not disabled
            if ((prState1 != null) && (prState2 != null)){
                if (prState2.getValue()){
                    drId = prState1.getValue()?drId4:drId3;
                } else {
                    drId = prState1.getValue()?drId2:drId1;
                }
            } else if (prState1 != null){
                drId = prState1.getValue()?drId2:drId1;
            } else {
                drId = drId1;
            }
        }
        if (drId == 0){
            setCompoundDrawables(null,null,null,null);
        } else {
            Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drId, getContext().getTheme());
            if (drawable != null){
                drawable.setBounds(0,0,drawableSize,drawableSize);
                setCompoundDrawables(drawable,null,null,null);
            }
        }
    }

}