package nachos.vm;

import nachos.machine.TranslationEntry;

import java.util.Objects;

public class TranslationEntryDecorators {
    int pid;
    TranslationEntry translationEntry;

    public TranslationEntryDecorators(int pid, TranslationEntry translationEntry) {
        this.pid = pid;
        this.translationEntry = translationEntry;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public TranslationEntry getTranslationEntry() {
        return translationEntry;
    }

    public void setTranslationEntry(TranslationEntry translationEntry) {
        this.translationEntry = translationEntry;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranslationEntryDecorators that = (TranslationEntryDecorators) o;
        return pid == that.pid && Objects.equals(translationEntry, that.translationEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, translationEntry);
    }
}
