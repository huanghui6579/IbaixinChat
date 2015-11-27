package net.ibaixin.chat.view.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.util.DialogUtils;

import net.ibaixin.chat.R;
import net.ibaixin.chat.activity.CommonAdapter;
import net.ibaixin.chat.model.ContextMenuItem;

import java.util.List;

/**
 * 菜单列表的适配器
 *
 * @author huanghui1
 * @update 2015年2月25日 下午5:55:39
 */
public class MenuItemAdapter extends CommonAdapter<ContextMenuItem> {

    final int itemColor;

    public MenuItemAdapter(List<ContextMenuItem> list, Context context) {
        super(list, context);
        itemColor = DialogUtils.resolveColor(context, R.attr.md_item_color, Color.BLACK);
    }

    @Override
    public long getItemId(int position) {
        ContextMenuItem item = (ContextMenuItem) getItem(position);
        return item.getItemId();
    }

    @Override
    public boolean isEnabled(int position) {
        ContextMenuItem item = (ContextMenuItem) getItem(position);
        return item.isEnable();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MenuViewHoler holer = null;
        if (convertView == null) {
            holer = new MenuViewHoler();
            convertView = inflater.inflate(R.layout.md_listitem, parent, false);

            holer.textView = (TextView) convertView.findViewById(R.id.title);
            convertView.setTag(holer);
        } else {
            holer = (MenuViewHoler) convertView.getTag();
        }
        ContextMenuItem item = list.get(position);
        holer.textView.setText(item.getTitle());
        holer.textView.setTextColor(itemColor);
        holer.textView.setTag(item.getItemId() + ":" + item.getTitle());
        return convertView;
    }

    public final class MenuViewHoler {
        public TextView textView;
    }
}