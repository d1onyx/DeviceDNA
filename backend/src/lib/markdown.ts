const escapeHtml = (value: string): string =>
  value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const headingId = (value: string): string =>
  value
    .trim()
    .toLocaleLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, "-")
    .replace(/^-|-$/g, "");

const renderInline = (value: string): string =>
  escapeHtml(value)
    .replace(/\[([^\]]+)]\((#[^)]+)\)/g, '<a href="$2">$1</a>')
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>");

/** Renders the small, trusted Markdown subset used by PRIVACY_POLICY.md. */
export const renderPolicyMarkdown = (markdown: string): string => {
  const output: string[] = [];
  let paragraph: string[] = [];
  let list: "ul" | "ol" | null = null;

  const closeParagraph = () => {
    if (paragraph.length > 0) {
      output.push(`<p>${renderInline(paragraph.join(" "))}</p>`);
      paragraph = [];
    }
  };
  const closeList = () => {
    if (list !== null) {
      output.push(`</${list}>`);
      list = null;
    }
  };

  for (const sourceLine of markdown.split(/\r?\n/)) {
    const line = sourceLine.trim();
    if (line.length === 0) {
      closeParagraph();
      closeList();
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(line);
    if (heading) {
      closeParagraph();
      closeList();
      const level = heading[1].length;
      const title = heading[2];
      output.push(`<h${level} id="${headingId(title)}">${renderInline(title)}</h${level}>`);
      continue;
    }

    const unorderedItem = /^-\s+(.+)$/.exec(line);
    const orderedItem = /^\d+\.\s+(.+)$/.exec(line);
    if (unorderedItem || orderedItem) {
      closeParagraph();
      const nextList = unorderedItem ? "ul" : "ol";
      if (list !== nextList) {
        closeList();
        list = nextList;
        output.push(`<${list}>`);
      }
      output.push(`<li>${renderInline((unorderedItem ?? orderedItem)![1])}</li>`);
      continue;
    }

    closeList();
    paragraph.push(line);
  }

  closeParagraph();
  closeList();
  return output.join("\n");
};
