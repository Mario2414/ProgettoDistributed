package progetto.utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class ListenerList<T> extends LinkedBlockingDeque<WeakReference<T>> {
    public ListenerList() {
        super();
    }

    public ListenerList(Collection<? extends WeakReference<T>> c) {
        super(c);
    }

    public void addListener(T t) {
        super.add(new WeakReference<>(t));
    }

    public void forEachListeners(Consumer<T> action) {
        Iterator<WeakReference<T>> iterator = this.iterator();
        while (iterator.hasNext()) {
            T value = iterator.next().get();
            if(value != null) {
                action.accept(value);
            } else {
                iterator.remove();
            }
        }
    }

    public List<T> clone() {
        List<T> arrayList = new ArrayList<>(this.size());
        this.forEachListeners(l -> arrayList.add(l));
        return arrayList;
    }
}
