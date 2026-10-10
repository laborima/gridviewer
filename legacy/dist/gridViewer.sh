#/bin/sh

INSTALLDIR=/home/mercator_px/versions/gridviewer
#    *  win32: -Djava.library.path=INSTALLDIR\plugins\org.eclipse.swt.win32_2.0.0\os\win32\x86
#    * linux gtk: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.gtk_2.0.0/os/linux/x86
#    * linux motif: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.motif_2.0.0/os/linux/x86
#    * solaris motif: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.motif_2.0.0/os/solaris/sparc
#    * aix motif: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.motif_2.0.0/os/aix/ppc
#    * hpux motif: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.motif_2.0.0/os/hpux/PA_RISC
#    * photon qnx: -Djava.library.path=INSTALLDIR/eclipse/plugins/org.eclipse.swt.photon_2.0.0/os/qnx/x86 
SWT_LIB=$INSTALLDIR/lib/swt/linux
NETCDF_API=$INSTALLDIR/lib/netcdf
export MOZILLA_FIVE_HOME=/usr/lib/mozilla-1.7.12/
export LD_LIBRARY_PATH=$MOZILLA_FIVE_HOME:$LD_LIBRARY_PATH
#JAVA_HOME=/home/mlaborie/ENVDEVEL/jdk1.5.0_05/
#JAVA=$JAVA_HOME/bin/java 

JAVA_HOME=/home/mercator_px/versions/gridviewer/java/jre1.5.0_06
JAVA=$JAVA_HOME/bin/java

$JAVA -Djava.library.path=$SWT_LIB -jar $INSTALLDIR/gridViewer.jar $@
