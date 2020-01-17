package controller;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import model.Usuario;
import repository.UsuarioRepository;

@ManagedBean
@SessionScoped
public class UsuarioMB implements Serializable {

    private Usuario usuario = new Usuario();
    private UsuarioRepository repository = new UsuarioRepository();
    private String senhaComfirmacao;
    private String senhaAtual;
    private String novaSenha;
    private String novoEmail;

    public String validarLogin() {
        Usuario usuarioValidar = new Usuario(usuario.getEmail(), usuario.getSenha());
        usuario = repository.getUsuario(usuarioValidar.getEmail());
        if (usuarioValidar.getSenha().equals(usuario.getSenha())) {
            return "main.jsf";
        }
        enviarMensagem(FacesMessage.SEVERITY_ERROR, null, "Senha ou email incorretos");
        return null;
    }

    public void sair() {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("usuarioMB");
    }
    
    private void enviarMensagem(FacesMessage.Severity type, String summary, String detail) {
        FacesMessage msg = new FacesMessage(type, summary, detail);
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String verificarSenha() {
        if (usuario.getId() != null) {
            if (senhaAtual.equals(usuario.getSenha())) {
                if (novaSenha.equals(senhaComfirmacao)) {
                    usuario.setSenha(novaSenha);
                    alterarUsuario();
                    limparString();
                    return null;
                } else {
                    enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", 
                            "Nova senha diferente da confirmação");
                    return null;
                }
            } else {
                enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", 
                        "Senha atual incorreta");
                return null;
            }
        } else if (!verificarContaExistente()) {
            if (usuario.getSenha().equals(senhaComfirmacao)) {
                cadastrarUsuario();
                return "cadastroRealizado.jsf";
            }
            enviarMensagem(FacesMessage.SEVERITY_ERROR, null, "Senha incorreta");
            return null;
        }

        enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Email já registrado");
        return null;
    }

    public void limparString() {
        novaSenha = new String();
        senhaAtual = new String();
        senhaComfirmacao = new String();
        novoEmail = new String();
    }
    
    public boolean verificarContaExistente() {
        return repository.verificarContaExiste(usuario.getEmail());
    }

    public void verificarEmail() {
        if (repository.verificarContaExiste(novoEmail)) {
            enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", 
                    "Email já registrado em outra conta");
        } else {
            usuario.setEmail(novoEmail);
            alterarUsuario();
            limparString();
        }
    }
    
    public void cadastrarUsuario() {
        try {
            repository.addUsuario(usuario);
            Path path = Paths.get("C:\\uploads\\" + usuario.getId());
            //if directory exists?
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            e.getStackTrace();
            enviarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Conta não registrada");
        }
    }

    public void alterarUsuario() {
        repository.alterar(usuario);
        enviarMensagem(FacesMessage.SEVERITY_INFO, "Ok", "Alterado com sucesso!");
    }

    public UsuarioRepository getRepository() {
        return repository;
    }

    public void setRepository(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getSenhaComfirmacao() {
        return senhaComfirmacao;
    }

    public void setSenhaComfirmacao(String senhaComfirmacao) {
        this.senhaComfirmacao = senhaComfirmacao;
    }

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getNovoEmail() {
        return novoEmail;
    }

    public void setNovoEmail(String novoEmail) {
        this.novoEmail = novoEmail;
    }

}
