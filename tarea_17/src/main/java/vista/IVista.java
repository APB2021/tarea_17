package vista;

import modelo.AlumnosDAO;

public interface IVista {

	public void mostrarMenu(AlumnosDAO modelo);

	// case 1:
	public void insertarNuevoAlumno(AlumnosDAO modelo);

	// case 2:
	public void insertarNuevoGrupo(AlumnosDAO modelo);

	// cases 3 y 12:
	public void mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion);

	// case 4:
	public void guardarAlumnosEnFicheroTexto();

	// case 5:
	public void leerAlumnosDesdeFichero();

	// case 6:
	public void modificarNombreAlumnoPorNia();

	// case 7:
	public void eliminarAlumnoPorNIA();

	// case 8:
	public void eliminarAlumnosPorGrupo();

	// case 9:
	public void guardarGruposEnXML();

	// case 10:
	public void leerYGuardarGruposXML();

	// case 11:
	public void mostrarAlumnosPorGrupo(AlumnosDAO modelo);

	// case 13:
	public void cambiarGrupoAlumno(AlumnosDAO modelo);

	// case 14:
	public void guardarGrupoEspecificoEnXML(AlumnosDAO modelo);
}