package org.testcontainers.hivemq;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchiveFormat;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.ServiceExtensionLoader;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.impl.base.spec.JavaArchiveImpl;

import java.util.Arrays;

class ShadingAwareServiceExtensionLoader extends ServiceExtensionLoader {
    {
        addOverride(JavaArchive.class, JavaArchiveImpl.class);
        addOverride(ZipExporter.class, ZipExporterImpl.class);
    }

    public ShadingAwareServiceExtensionLoader() throws IllegalArgumentException {
        super(Arrays.asList(ClassLoader.getSystemClassLoader()));
    }

    @Override
    public <T extends Assignable> String getExtensionFromExtensionMapping(Class<T> type) {
        if (type == JavaArchive.class) {
            return ".jar";
        }
        return super.getExtensionFromExtensionMapping(type);
    }

    @Override
    public <T extends Archive<T>> ArchiveFormat getArchiveFormatFromExtensionMapping(Class<T> type) {
        if (type == JavaArchive.class) {
            return ArchiveFormat.ZIP;
        }
        return super.getArchiveFormatFromExtensionMapping(type);
    }
}
