/**
 * Endereços das APIs.
 *
 * Ficam num arquivo só porque estavam repetidos e escritos à mão em cada
 * chamada, e um deles apontava para o serviço errado: a listagem de produtos
 * pedia na porta do auth-service, então a vitrine nunca carregava.
 *
 * Trocar de ambiente significa editar este arquivo, ou substituí-lo na
 * publicação. Não há passo de build para injetar variável de ambiente.
 */
const API = {
  auth: 'http://localhost:8080',
  product: 'http://localhost:8081',
};
