package wix3y.dungeonDesigner.util.datastructures;

public class AmountWriteChecked {
    private int amount;
    private boolean dirty;

    public AmountWriteChecked(int amount, boolean dirty) {
        this.amount = amount;
        this.dirty = dirty;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty){
        this.dirty = dirty;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
