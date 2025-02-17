package ejecutadores;

import controlador.Controlador;


//import modelo.AlumnosBD;
import modelo.AlumnosDAO;
//import modelo.AlumnosFicheroXML;
import modelo.AlumnosHibernate;
import vista.IVista;
import vista.VistaConsola;

public class Ejecutador4 {

	public static void main(String[] args) {
		AlumnosDAO modelo = new AlumnosHibernate();
		IVista vista = new VistaConsola();
		new Controlador().ejecutar(modelo, vista);
		
	}
}
