package com.app.truewebapp.data.dto.product;

import com.app.accutecherp.data.dto.product.product_group;

import java.util.ArrayList;
import java.util.List;

public class SrvcSeekDataModal {
    private boolean selected = true;
    private boolean checked;
    com.app.accutecherp.data.dto.product.product_group product_group;
    private ArrayList<List> allItemInSection;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public com.app.accutecherp.data.dto.product.product_group getProduct_group() {
        return product_group;
    }

    public void setProduct_group(com.app.accutecherp.data.dto.product.product_group product_group) {
        this.product_group = product_group;
    }

    public ArrayList<List> getAllItemInSection() {
        return allItemInSection;
    }

    public void setAllItemInSection(ArrayList<List> allItemInSection) {
        this.allItemInSection = allItemInSection;
    }
}
