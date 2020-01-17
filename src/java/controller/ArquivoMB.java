package controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import model.Arquivo;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import repository.ArquivoRepository;

@ManagedBean
@ViewScoped
public class ArquivoMB implements Serializable {

    private Arquivo arquivo = new Arquivo();
    private Arquivo pastaNova = new Arquivo();
    private Arquivo selecionado = new Arquivo();
    private ArquivoRepository repository = new ArquivoRepository();
    private List<Arquivo> arquivos = new ArrayList<>();
    private Part file;
    private StreamedContent fileDownload;
    private String diretorioAtual;

    @ManagedProperty(value = "#{usuarioMB}")
    private UsuarioMB usuarioMB;

    @PostConstruct
    public void carregarUsuario() {
        arquivo.setUsuario(usuarioMB.getUsuario());
        carregarTabela(true);
    }

    public void carregarTabela(boolean inicio) {
        if (inicio) {
            arquivos = repository.getArquivos(
                    "c:/uploads/" + arquivo.getUsuario().getId(), inicio);
        } else {
            arquivos = repository.getArquivos(
                    selecionado.getDiretorio() + "/" + selecionado.getNome(), inicio);
        }
    }

    private void enviarMensagem(FacesMessage.Severity type, String summary, String detail) {
        FacesMessage msg = new FacesMessage(type, summary, detail);
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public StreamedContent getFileDownload() throws FileNotFoundException, IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        if (!selecionado.getTipo().equals("Pasta")) {
            String fileName = selecionado.getNome() + selecionado.getTipo();

            InputStream stream = new FileInputStream(
                    selecionado.getDiretorio().replace("/", "\\") + "\\" + fileName);

            return fileDownload = new DefaultStreamedContent(stream, ec.getMimeType(fileName), fileName);
        }

        List<Arquivo> arquivosToDownload = new ArrayList<>();
        FileOutputStream fos = new FileOutputStream(
                selecionado.getDiretorio().replace("/", "\\") + "\\"
                + selecionado.getNome() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(fos);

        arquivosToDownload = repository.getArquivosToDownload(selecionado.getDiretorio() + "/"
                + selecionado.getNome());

        for (int i = 0; i < arquivosToDownload.size(); i++) {
            String fileName = arquivosToDownload.get(i).getNome()
                    + arquivosToDownload.get(i).getTipo();

            addToZipFile(arquivosToDownload.get(i).getDiretorio()
                    .replace("/", "\\") + "\\" + fileName, zos);
        }
        zos.close();
        fos.close();
        String name = selecionado.getNome() + ".zip";

        InputStream stream = new FileInputStream(
                selecionado.getDiretorio().replace("/", "\\") + "\\" + name);

        return fileDownload = new DefaultStreamedContent(stream, ec.getMimeType(name), name);

    }

    public void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException {

        File fileTemp = new File(fileName);
        try (FileInputStream fis = new FileInputStream(fileTemp)) {
            ZipEntry zipEntry = new ZipEntry(fileName
                    .substring(fileName
                            .lastIndexOf(selecionado.getNome()), fileName.length()));
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
        }
    }

    public void submit() throws ServletException, IOException {
        for (Part part : getAllParts(file)) {
            Arquivo arquivo2 = new Arquivo();
            String fileName = part.getSubmittedFileName();

            arquivo2.setUsuario(arquivo.getUsuario());
            arquivo2.setNome(fileName.substring(0, fileName.lastIndexOf(".")));
            arquivo2.setTamanho(part.getSize());
            arquivo2.setTipo(fileName.substring(fileName.lastIndexOf("."), fileName.length()));
            if (diretorioAtual.isEmpty() || diretorioAtual == null) {
                arquivo2.setDiretorio("c:\\uploads\\" + arquivo.getUsuario().getId());
            } else {
                arquivo2.setDiretorio(diretorioAtual.replace("/", "\\"));
            }

            String diretorioTemp = arquivo2.getDiretorio().replace("\\", "/");
            if (repository.verificarArquivoExiste(diretorioTemp, arquivo2.getNome(), arquivo2.getTipo())) {
                enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Nome \""
                        + arquivo2.getNome() + arquivo2.getTipo() + "\" já existe");
                selecionado = new Arquivo();
            } else {
                try (InputStream fileContent = part.getInputStream()) {
                    Files.copy(fileContent,
                            new File(arquivo2.getDiretorio(), fileName).toPath());
                } catch (Exception e) {
                    e.getStackTrace();
                }

                arquivo2.setDiretorio(arquivo2.getDiretorio().replace("\\", "/"));
                repository.addArquivo(arquivo2);
            }

        }

        if (diretorioAtual.isEmpty() || diretorioAtual == null) {
            carregarTabela(true);
        } else {
            carregarTabela(false);
        }
    }

    private boolean verificarList(List<String> list, String nome) {
        for (int i = 0; i < list.size(); i++) {
            if (nome.equals(list.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void verificarPastaExiste() throws ServletException, IOException {

        String diretorioTemp;
        if (diretorioAtual.isEmpty() || diretorioAtual == null) {
            diretorioTemp = "c:/uploads/" + arquivo.getUsuario().getId();
        } else {
            diretorioTemp = diretorioAtual;
        }

        String nome = file.getSubmittedFileName();

        if (repository.verificarArquivoExiste(diretorioTemp,
                nome.substring(0, nome.indexOf("/")),
                "Pasta")) {
            enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Nome \""
                    + nome.substring(0, nome.indexOf("/")) + "\" já existe");
            selecionado = new Arquivo();
        } else {
            salvarPastas();
            carregarPastasArquivos();
        }
    }

    public void salvarPastas() throws ServletException, IOException {

        String nomePasta;
        List<String> pastas = new ArrayList<>();
        StringBuilder replaceTemp = new StringBuilder();
        int count = 0;
        int currentIndex = 0;

        for (Part part : getAllParts(file)) {
            nomePasta = new String();
            nomePasta = part.getSubmittedFileName();
            count = 0;
            for (int i = 0; i < nomePasta.length(); i++) {
                if (nomePasta.charAt(i) == '/') {
                    count++;
                }
            }

            if (count > 1) {
                String[] replaces = new String[count];
                for (int i = 0; i < count; i++) {
                    replaces[i] = new String();
                    String addReplace = new String();

                    if (i == 0) {
                        replaces[i] = nomePasta;
                        addReplace = replaces[i].substring(0, replaces[i].indexOf('/'));

                        if (verificarList(pastas, addReplace)) {
                            pastas.add(addReplace);
                        }

                    } else {
                        try {
                            replaces[i] = replaces[i - 1].replaceFirst(replaceTemp + "/", "");
                            addReplace = replaces[i].substring(0, replaces[i].indexOf('/'));

                            if (verificarList(pastas, addReplace)) {
                                pastas.add(addReplace);
                            }

                        } catch (StringIndexOutOfBoundsException e) {
                            e.getStackTrace();
                            break;
                        }

                    }
                    replaceTemp.delete(0, replaceTemp.length());
                    replaceTemp.append(addReplace);
                }
            } else {
                String pasta = nomePasta.substring(0, nomePasta.indexOf("/"));
                if (verificarList(pastas, pasta)) {
                    pastas.add(pasta);
                }
            }

            for (int i = currentIndex; i < pastas.size(); i++) {
                Arquivo pasta = new Arquivo();
                pasta.setUsuario(arquivo.getUsuario());
                pasta.setNome(pastas.get(i));
                System.out.println(pastas.get(i));

                pasta.setTipo("Pasta");

                if (i == 0) {
                    if (diretorioAtual.isEmpty() || diretorioAtual == null) {
                        pasta.setDiretorio("c:/uploads/" + pasta.getUsuario().getId());
                    } else {
                        pasta.setDiretorio(diretorioAtual);
                    }

                } else {
                    String copia1 = new String();
                    String copia2 = new String();

                    replaceTemp.delete(0, replaceTemp.length());
                    copia1 = nomePasta.replace(pastas.get(i), "");
                    copia2 = copia1.replace("//", "|");
                    replaceTemp.append(copia2);

                    if (diretorioAtual.isEmpty() || diretorioAtual == null) {
                        pasta.setDiretorio("c:/uploads/" + pasta.getUsuario().getId() + "/"
                                + replaceTemp.substring(0, replaceTemp.lastIndexOf("|")));
                    } else {
                        pasta.setDiretorio(diretorioAtual + "/"
                                + replaceTemp.substring(0, replaceTemp.lastIndexOf("|")));
                    }
                }

                repository.addArquivo(pasta);
                currentIndex = pastas.size();
            }
        }
    }

    public void carregarPastasArquivos() throws ServletException, IOException {
        for (Part part : getAllParts(file)) {
            Arquivo arquivo2 = new Arquivo();
            String fileName = part.getSubmittedFileName();
            String name = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());

            arquivo2.setUsuario(arquivo.getUsuario());
            arquivo2.setNome(name.substring(0, name.lastIndexOf(".")));
            arquivo2.setTamanho(part.getSize());
            arquivo2.setTipo(fileName.substring(fileName.lastIndexOf("."), fileName.length()));

            Path path;

            if (diretorioAtual.isEmpty() || diretorioAtual == null) {
                path = Paths.get("c:\\uploads\\" + arquivo2.getUsuario().getId() + "\\"
                        + fileName.substring(0, fileName.lastIndexOf('/')));
            } else {
                path = Paths.get(diretorioAtual.replace("/", "\\") + "\\"
                        + fileName.substring(0, fileName.lastIndexOf('/')));
            }

            arquivo2.setDiretorio(path.toString().replace("\\", "/"));

            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            try (InputStream fileContent = part.getInputStream()) {
                Files.copy(fileContent,
                        new File(path.toString(), name).toPath());
            } catch (Exception e) {
                e.getStackTrace();
            }

            repository.addArquivo(arquivo2);
        }

        if (diretorioAtual.isEmpty() || diretorioAtual == null) {
            carregarTabela(true);
        } else {
            carregarTabela(false);
        }
    }

    public void deletar() throws IOException {
        if (!selecionado.getTipo().equals("Pasta")) {
            repository.excluir(selecionado);
            String nomeArquivo = selecionado.getNome() + selecionado.getTipo();
            Path path = Paths.get(selecionado.getDiretorio().replace("/", "\\")
                    + "\\" + nomeArquivo);
            try {
                Files.delete(path);
            } catch (IOException ex) {
                ex.getStackTrace();
            }
            selecionado = new Arquivo();
            carregarTabela(true);
        } else {
            deletarPasta();
        }
    }

    public void deletarPasta() throws IOException {
        repository.excluirPasta(selecionado.getDiretorio() + "/" + selecionado.getNome());
        repository.excluir(selecionado);
        FileUtils.deleteDirectory(new File(selecionado.getDiretorio().replace("/", "\\") + "\\"
                + selecionado.getNome()));

        selecionado = new Arquivo();
        carregarTabela(true);
    }

    public void novaPasta() {
        boolean inicial;
        pastaNova.setTipo("Pasta");
        pastaNova.setUsuario(arquivo.getUsuario());
        if (diretorioAtual.isEmpty() || diretorioAtual == null) {
            pastaNova.setDiretorio("c:/uploads/" + arquivo.getUsuario().getId());
            inicial = true;
        } else {
            pastaNova.setDiretorio(diretorioAtual);
            inicial = false;
        }

        if (repository.verificarArquivoExiste(pastaNova.getDiretorio(), pastaNova.getNome(), "Pasta")) {
            enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Nome \""
                    + pastaNova.getNome() + "\" já existe");
            selecionado = new Arquivo();
        } else {
            new File(pastaNova.getDiretorio().replace("/", "\\") + "\\" + pastaNova.getNome()).mkdirs();
            repository.addArquivo(pastaNova);
            pastaNova = new Arquivo();
            carregarTabela(inicial);
        }

    }

    public static Collection<Part> getAllParts(Part part) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return request.getParts().stream().filter(p -> part.getName().equals(p.getName())).collect(Collectors.toList());
    }

    public ArquivoRepository getRepository() {
        return repository;
    }

    public void setRepository(ArquivoRepository repository) {
        this.repository = repository;
    }

    public Part getFile() {
        return null;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    public Arquivo getArquivo() {
        return arquivo;
    }

    public void setArquivo(Arquivo arquivo) {
        this.arquivo = arquivo;
    }

    public Arquivo getSelecionado() {
        return selecionado;
    }

    public void setSelecionado(Arquivo selecionado) {
        this.selecionado = selecionado;
    }

    public List<Arquivo> getArquivos() {
        return arquivos;
    }

    public void setArquivos(List<Arquivo> arquivos) {
        this.arquivos = arquivos;
    }

    public UsuarioMB getUsuarioMB() {
        return usuarioMB;
    }

    public void setUsuarioMB(UsuarioMB usuarioMB) {
        this.usuarioMB = usuarioMB;
    }

    public Arquivo getPastaNova() {
        return pastaNova;
    }

    public void setPastaNova(Arquivo pastaNova) {
        this.pastaNova = pastaNova;
    }

    public String getDiretorioAtual() {
        if (selecionado.getDiretorio() == null) {
            diretorioAtual = "";
        } else {
            diretorioAtual = selecionado.getDiretorio() + "/" + selecionado.getNome();
        }
        return diretorioAtual;
    }

    public void setDiretorioAtual(String diretorioAtual) {
        this.diretorioAtual = diretorioAtual;
    }

}
