package br.dcx.ufpb.oliveira.artur;

public class Conta {
    private String login;
    private String senha;
    private String emailrecuperacao;
    private boolean infamily;
    private boolean temminecraft;

    public Conta(){

        this.login = "microsoftlogin01@outlook.com";
        this.senha = "00000";
        this.emailrecuperacao = "";
        this.infamily = false;
        this.temminecraft = false;


    }
    public Conta(String login, String senha, String emailrecuperacao, boolean infamily, boolean temminecraft){
    this.login = login;
    this.senha = senha;
    this.emailrecuperacao = emailrecuperacao;
    this.infamily = infamily;
    this.temminecraft = temminecraft;

    }
    public String getLogin(){
        return login;
    }

}
