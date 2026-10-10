package configManagerView;


public class configManagerData {

 private String   filein_bathy_parent;
 private String   varName;
 private String   filein_coord_parent;
 private String   fileout_bathy_child;
 private String   fileout_coord_child;
 private String   filein_bathy_child;
 private String   filein_coord_child;
 private boolean  coord;
 private boolean  interp;
 private boolean  match;
 private int      nmatch ;
 private boolean  fill_closed;
 

 
 private int   jpizoom;
 private int   jpjzoom;
 private int   jpiglo;
 private int   jpjglo;   
 private int   imin;
 private int   imax;
 private int   jmin;
 private int   jmax;
 private int   nraff;

 
 public static configManagerData getInstance() {
     if (null == instance) { // Premier appel
         instance = new configManagerData();
     }
     return instance;
 }

 
 public String getRequest(){
	 
	 
	/* sed -e "s:FILEINBATHY0:$1:" \
	    -e "s:FILEOUTBATHY1:$2:" \
	    -e "s:FILEINBATHY1:$3:" \    
	    -e "s:FILEINCOORD0:$4:" \
	    -e "s:FILEOUTCOORD1:$5:" \
	    -e "s:FILEINCOORD1:$6:" \
	    -e "s:VARCOORD:$7:" \
	    -e "s:VARINTERP:$8:" \
	    -e "s:VARMATCH:$9:" \
	    -e "s:VARNMATCH:${10}:" \
	    -e "s:VARFILLCLOSED:${11}:" \
	    -e "s:VARIMIN:${12}:" \
	    -e "s:VARIMAX:${13}:" \
	    -e "s:VARJMIN:${14}:" \
	    -e "s:VARJMAX:${15}:" \
	    -e "s:VARIZOOM:${16}:" \
	    -e "s:VARJZOOM:${17}:" \
	    -e "s:VARIGLO:${18}:" \
	    -e "s:VARJGLO:${19}:" \
	    -e "s:VARNRAFF:${20}:" ./refine_bathy.in.skel > ./refine_bathy.in */
	
	 
		String request = " "
			+filein_bathy_parent+" "
			+fileout_bathy_child+" "
			+filein_bathy_child+" "
			+filein_coord_parent+" "
			+fileout_coord_child+" "
			+filein_coord_child+" "
			+" ."+coord+"."
			+" ."+interp+"."
			+" ."+match+"."
			+" "+nmatch+" "
			+" ."+fill_closed+". "
			+imin+" "
			+imax+" "
			+jmin+" "
			+jmax+" "
			+jpizoom+" "
			+jpjzoom+" "
			+jpiglo+" "
			+jpjglo+" "
			+nraff+" ";
	    return request;
 }
 
 /** Constructeur redéfini comme étant privé pour interdire
 * son appel et forcer à passer par la méthode <link
 */
 private configManagerData() {
 }

 /** L'instance statique */
 private static configManagerData instance;
 
public boolean getCoord() {
	return coord;
}
public void setCoord(boolean coord) {
	this.coord = coord;
}
public String getFilein_bathy_child() {
	return filein_bathy_child;
}
public void setFilein_bathy_child(String filein_bathy_child) {
	this.filein_bathy_child = filein_bathy_child;
}
public String getFilein_bathy_parent() {
	return filein_bathy_parent;
}
public void setFilein_bathy_parent(String filein_bathy_parent) {
	this.filein_bathy_parent = filein_bathy_parent;
}
public String getFilein_coord_child() {
	return filein_coord_child;
}
public void setFilein_coord_child(String filein_coord_child) {
	this.filein_coord_child = filein_coord_child;
}
public String getFilein_coord_parent() {
	return filein_coord_parent;
}
public void setFilein_coord_parent(String filein_coord_parent) {
	this.filein_coord_parent = filein_coord_parent;
}
public String getFileout_bathy_child() {
	return fileout_bathy_child;
}
public void setFileout_bathy_child(String fileout_bathy_child) {
	this.fileout_bathy_child = fileout_bathy_child;
}
public String getFileout_coord_child() {
	return fileout_coord_child;
}
public void setFileout_coord_child(String fileout_coord_child) {
	this.fileout_coord_child = fileout_coord_child;
}
public boolean getFill_closed() {
	return fill_closed;
}
public void setFill_closed(boolean fill_closed) {
	this.fill_closed = fill_closed;
}
public int getImax() {
	return imax;
}
public void setImax(int imax) {
	this.imax = imax;
}
public int getImin() {
	return imin;
}
public void setImin(int imin) {
	this.imin = imin;
}
public boolean getInterp() {
	return interp;
}
public void setInterp(boolean interp) {
	this.interp = interp;
}
public int getJmax() {
	return jmax;
}
public void setJmax(int jmax) {
	this.jmax = jmax;
}
public int getJmin() {
	return jmin;
}
public void setJmin(int jmin) {
	this.jmin = jmin;
}
public int getJpiglo() {
	return jpiglo;
}
public void setJpiglo(int jpiglo) {
	this.jpiglo = jpiglo;
}
public int getJpizoom() {
	return jpizoom;
}
public void setJpizoom(int jpizoom) {
	this.jpizoom = jpizoom;
}
public int getJpjglo() {
	return jpjglo;
}
public void setJpjglo(int jpjglo) {
	this.jpjglo = jpjglo;
}
public int getJpjzoom() {
	return jpjzoom;
}
public void setJpjzoom(int jpjzoom) {
	this.jpjzoom = jpjzoom;
}
public boolean getMatch() {
	return match;
}
public void setMatch(boolean match) {
	this.match = match;
}
public int getNmatch() {
	return nmatch;
}
public void setNmatch(int nmatch) {
	this.nmatch = nmatch;
}
public int getNraff() {
	return nraff;
}
public void setNraff(int nraff) {
	this.nraff = nraff;
}
public String getVarName() {
	return varName;
}
public void setVarName(String varName) {
	this.varName = varName;
}
 
 
}
