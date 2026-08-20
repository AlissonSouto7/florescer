'use client';

import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';

import { alterarPlanta, errosDe, salvarPlanta, type Planta } from '@/lib/api';
import { AMBIENTE, DIFICULDADE, LUMINOSIDADE, LUMINOSIDADE_DETALHE, REGA, caminhoDaImagem } from '@/lib/rotulos';
import { lerToken } from '@/lib/sessao';
import { Select } from './Select';

/**
 * Cadastro de planta.
 *
 * Pensado para quem não é técnico: as opções já vêm marcadas nos valores mais
 * comuns, o preço aceita vírgula, e a foto aparece antes de salvar. Sete campos
 * a mais não podem virar sete decisões a mais por planta.
 */
export function FormularioPlanta({ planta }: { planta?: Planta }) {
  const router = useRouter();
  const [imagem, setImagem] = useState<File | null>(null);
  const [previa, setPrevia] = useState<string | null>(planta ? caminhoDaImagem(planta.imageUrl) : null);
  const [erros, setErros] = useState<Record<string, string>>({});
  const [enviando, setEnviando] = useState(false);
  const urlDaPrevia = useRef<string | null>(null);

  // A prévia é uma URL de objeto criada na memória do navegador. Sem revogar,
  // cada troca de foto deixa a anterior ocupando memória até a aba fechar.
  useEffect(() => {
    return () => {
      if (urlDaPrevia.current) URL.revokeObjectURL(urlDaPrevia.current);
    };
  }, []);

  function aoEscolherImagem(evento: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0] ?? null;
    setImagem(arquivo);

    if (urlDaPrevia.current) URL.revokeObjectURL(urlDaPrevia.current);

    if (arquivo) {
      const url = URL.createObjectURL(arquivo);
      urlDaPrevia.current = url;
      setPrevia(url);
    } else {
      setPrevia(planta ? caminhoDaImagem(planta.imageUrl) : null);
    }
  }

  async function aoEnviar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    setErros({});

    const token = lerToken();
    if (!token) {
      router.push('/login');
      return;
    }

    const form = new FormData(evento.currentTarget);
    const dados = {
      name: form.get('name'),
      type: form.get('type'),
      description: form.get('description'),
      // A pessoa digita 49,90 como fala. Converter aqui evita obrigá-la a
      // lembrar que o computador quer ponto.
      price: Number(String(form.get('price')).replace(',', '.')),
      quantityStock: Number(form.get('quantityStock')),
      careRequirements: form.get('careRequirements'),
      availability: form.get('availability') === 'on',
      status: form.get('status'),
      heightCm: Number(form.get('heightCm')),
      light: form.get('light'),
      watering: form.get('watering'),
      petSafe: form.get('petSafe') === 'on',
      environment: form.get('environment'),
      difficulty: form.get('difficulty'),
      includesPot: form.get('includesPot') === 'on',
    };

    if (!planta && !imagem) {
      setErros({ image: 'Escolha uma foto da planta.' });
      return;
    }

    setEnviando(true);
    try {
      // Cadastro manda tudo; edição manda o mesmo conjunto por PATCH, que no
      // backend altera apenas o que veio preenchido.
      const resposta = planta
        ? await alterarPlanta(planta.id, dados, imagem, token)
        : await salvarPlanta(dados, imagem, token);

      if (resposta.ok) {
        router.push('/admin');
        router.refresh();
        return;
      }

      if (resposta.status === 401) {
        router.push('/login');
        return;
      }

      // A API devolve a mensagem por campo. Mostrar cada uma ao lado do input
      // certo poupa a pessoa de procurar o que errou.
      setErros(await errosDe(resposta));
    } catch {
      setErros({ _geral: 'Não foi possível salvar. Verifique sua conexão.' });
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={aoEnviar} className="space-y-6">
      {erros._geral && (
        <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {erros._geral}
        </p>
      )}

      <Secao titulo="A planta">
        <Campo id="name" rotulo="Nome" erro={erros.name} obrigatorio>
          <input id="name" name="name" required defaultValue={planta?.name} className={ESTILO_INPUT} />
        </Campo>

        <Campo id="type" rotulo="Tipo" dica="Suculenta, pendente, flor..." erro={erros.type} obrigatorio>
          <input id="type" name="type" required defaultValue={planta?.type} className={ESTILO_INPUT} />
        </Campo>

        <Campo id="description" rotulo="Descrição" erro={erros.description} obrigatorio>
          <textarea
            id="description"
            name="description"
            required
            rows={3}
            defaultValue={planta?.description}
            className={ESTILO_INPUT}
          />
        </Campo>
      </Secao>

      <Secao titulo="Preço e estoque">
        <div className="grid grid-cols-2 gap-4">
          <Campo id="price" rotulo="Preço" dica="Ex.: 49,90" erro={erros.price} obrigatorio>
            <input
              id="price"
              name="price"
              required
              inputMode="decimal"
              defaultValue={planta?.price?.toString().replace('.', ',')}
              className={ESTILO_INPUT}
            />
          </Campo>

          <Campo id="quantityStock" rotulo="Quantas você tem" erro={erros.quantityStock} obrigatorio>
            <input
              id="quantityStock"
              name="quantityStock"
              type="number"
              min={0}
              required
              defaultValue={planta?.quantityStock ?? 1}
              className={ESTILO_INPUT}
            />
          </Campo>
        </div>

        <div className="flex flex-wrap gap-6">
          <Marcador nome="availability" rotulo="À venda" marcadoPorPadrao={planta?.availability ?? true} />
          <Marcador nome="includesPot" rotulo="Vai com o vaso" marcadoPorPadrao={planta?.includesPot ?? true} />
        </div>

        <Campo id="status" rotulo="Situação" erro={erros.status}>
          <Select id="status" name="status" defaultValue={planta?.status ?? 'ATIVO'}>
            <option value="ATIVO">Aparece na vitrine</option>
            <option value="INATIVO">Escondida</option>
            <option value="ESGOTADO">Esgotada</option>
          </Select>
        </Campo>
      </Secao>

      <Secao titulo="O que quem compra pergunta">
        <Campo id="heightCm" rotulo="Altura em centímetros" dica="Aproximada, com o vaso" erro={erros.heightCm} obrigatorio>
          <input
            id="heightCm"
            name="heightCm"
            type="number"
            min={1}
            max={1000}
            required
            defaultValue={planta?.heightCm ?? 30}
            className={ESTILO_INPUT}
          />
        </Campo>

        <Campo id="light" rotulo="Quanta luz ela aguenta" erro={erros.light} obrigatorio>
          <Select id="light" name="light" defaultValue={planta?.light ?? 'MEIA_SOMBRA'}>
            {Object.entries(LUMINOSIDADE).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>
                {rotulo} &mdash; {LUMINOSIDADE_DETALHE[valor as keyof typeof LUMINOSIDADE_DETALHE]}
              </option>
            ))}
          </Select>
        </Campo>

        <Campo id="watering" rotulo="De quanto em quanto tempo regar" erro={erros.watering} obrigatorio>
          <Select id="watering" name="watering" defaultValue={planta?.watering ?? 'SEMANAL'}>
            {Object.entries(REGA).map(([valor, rotulo]) => (
              <option key={valor} value={valor}>{rotulo}</option>
            ))}
          </Select>
        </Campo>

        <div className="grid grid-cols-2 gap-4">
          <Campo id="environment" rotulo="Onde ela vive bem" erro={erros.environment} obrigatorio>
            <Select id="environment" name="environment" defaultValue={planta?.environment ?? 'INTERNO'}>
              {Object.entries(AMBIENTE).map(([valor, rotulo]) => (
                <option key={valor} value={valor}>{rotulo}</option>
              ))}
            </Select>
          </Campo>

          <Campo id="difficulty" rotulo="Dá trabalho?" erro={erros.difficulty} obrigatorio>
            <Select id="difficulty" name="difficulty" defaultValue={planta?.difficulty ?? 'FACIL'}>
              {Object.entries(DIFICULDADE).map(([valor, rotulo]) => (
                <option key={valor} value={valor}>{rotulo}</option>
              ))}
            </Select>
          </Campo>
        </div>

        <Marcador
          nome="petSafe"
          rotulo="Segura para cães e gatos"
          dica="Se tiver dúvida, deixe desmarcado: quem tem animal precisa do aviso."
          marcadoPorPadrao={planta?.petSafe ?? false}
        />

        <Campo id="careRequirements" rotulo="Outros cuidados" erro={erros.careRequirements} obrigatorio>
          <textarea
            id="careRequirements"
            name="careRequirements"
            required
            rows={2}
            defaultValue={planta?.careRequirements}
            className={ESTILO_INPUT}
          />
        </Campo>
      </Secao>

      <Secao titulo="Foto">
        <Campo id="image" rotulo="Foto da planta" erro={erros.image} obrigatorio={!planta}>
          <input
            id="image"
            name="image"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={aoEscolherImagem}
            className="w-full text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-emerald-50
                       file:px-4 file:py-2 file:text-emerald-700 hover:file:bg-emerald-100"
          />
        </Campo>

        {/* A prévia existe para conferir que é a planta certa antes de salvar. */}
        {previa && (
          <div className="relative h-40 w-40 overflow-hidden rounded-lg border border-stone-200">
            <Image src={previa} alt="Prévia da foto escolhida" fill className="object-cover" unoptimized />
          </div>
        )}
      </Secao>

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={enviando}
          className="rounded-lg bg-emerald-600 px-6 py-2.5 font-medium text-white transition
                     hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-stone-300"
        >
          {enviando ? 'Salvando...' : planta ? 'Salvar alterações' : 'Cadastrar planta'}
        </button>
        <button
          type="button"
          onClick={() => router.push('/admin')}
          className="rounded-lg border border-stone-300 px-6 py-2.5 text-stone-700 hover:bg-stone-50"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}

const ESTILO_INPUT =
  'w-full rounded-lg border border-stone-300 px-3 py-2 focus:border-emerald-500 ' +
  'focus:outline-none focus:ring-1 focus:ring-emerald-500';

function Secao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <fieldset className="space-y-4 rounded-xl border border-stone-200 bg-white p-5">
      <legend className="px-2 text-sm font-semibold text-stone-500">{titulo}</legend>
      {children}
    </fieldset>
  );
}

function Campo({
  id,
  rotulo,
  dica,
  erro,
  obrigatorio,
  children,
}: {
  id: string;
  rotulo: string;
  dica?: string;
  erro?: string;
  obrigatorio?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-stone-700">
        {rotulo}
        {obrigatorio && <span className="ml-1 text-red-500">*</span>}
      </label>
      {dica && <p className="mb-1 text-xs text-stone-500">{dica}</p>}
      {children}
      {/* role="alert" faz o leitor de tela anunciar o erro assim que ele surge. */}
      {erro && (
        <p role="alert" className="mt-1 text-sm text-red-600">
          {erro}
        </p>
      )}
    </div>
  );
}

function Marcador({
  nome,
  rotulo,
  dica,
  marcadoPorPadrao,
}: {
  nome: string;
  rotulo: string;
  dica?: string;
  marcadoPorPadrao: boolean;
}) {
  return (
    <div>
      <label className="flex cursor-pointer items-center gap-2 text-sm text-stone-700">
        <input
          type="checkbox"
          name={nome}
          defaultChecked={marcadoPorPadrao}
          className="h-4 w-4 rounded border-stone-300 text-emerald-600 focus:ring-emerald-500"
        />
        {rotulo}
      </label>
      {dica && <p className="ml-6 text-xs text-stone-500">{dica}</p>}
    </div>
  );
}
