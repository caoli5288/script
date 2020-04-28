package com.mengcraft.script;

import com.mengcraft.script.util.Utils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import javax.script.Bindings;
import java.util.UUID;

/**
 * Created on 16-10-17.
 */
@EqualsAndHashCode(of = "uniqueId")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HandledTask implements Runnable {

    private final UUID uniqueId = UUID.randomUUID();

    private final ScriptPlugin plugin;
    private final Bindings invokableObj;
    private final int period;

    private Runnable complete;
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        long clock = System.currentTimeMillis();
        try {
            Utils.invoke(invokableObj);
        } finally {
            clock = System.currentTimeMillis() - clock;
            if (clock >= 5) {
                plugin.getLogger().warning(String.format("Tasks consume too much time. (%s millis)\n%s", clock, invokableObj.toString()));
            }
            if (period < 1) complete();
        }
    }

    public void complete() {
        if (cancel() && complete != null) {
            complete.run();
        }
    }

    public HandledTask complete(Runnable complete) {
        this.complete = complete;
        return this;
    }

    public boolean cancel() {
        return id > 0 && plugin.cancel(this);
    }

}
