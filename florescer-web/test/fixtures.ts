import type { Planta } from '@/lib/api';

/**
 * Uma planta completa, do jeito que a API devolve.
 *
 * Fica num lugar só porque os três testes de componente precisam dela inteira,
 * com os dezoito campos. Copiada em cada arquivo, um campo novo no backend
 * exigiria lembrar de atualizar todas as cópias, e a que ficasse para trás
 * passaria a testar um formato que não existe mais.
 *
 * Cada caso parte daqui e muda só o que interessa: `{ ...PLANTA, petSafe: true }`
 * deixa claro que o teste é sobre `petSafe`, e não sobre o resto.
 */
export const PLANTA: Planta = {
  id: 'planta-1',
  name: 'Espada de São Jorge',
  type: 'Folhagem',
  description: 'Resistente',
  price: 49.9,
  quantityStock: 3,
  careRequirements: 'Pouca água',
  availability: true,
  status: 'ATIVO',
  // Host da rede interna do Docker, de propósito: é o que a API devolve quando
  // a vitrine renderiza no servidor, e o navegador não alcança esse endereço.
  imageUrl: 'http://product-service:8081/uploads/a.png',
  heightCm: 40,
  light: 'MEIA_SOMBRA',
  watering: 'SEMANAL',
  petSafe: false,
  environment: 'INTERNO',
  difficulty: 'FACIL',
  includesPot: true,
};
