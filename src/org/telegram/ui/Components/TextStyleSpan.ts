/**
 * org.telegram.ui.Components.TextStyleSpan
 * Replicated directly from TextStyleSpan.java in DrKLO/Telegram Android
 */

export class TextStyleRun {
  public flags: number = 0;
  public start: number = 0;
  public end: number = 0;
  public url?: string;

  public copy(): TextStyleRun {
    const run = new TextStyleRun();
    run.flags = this.flags;
    run.start = this.start;
    run.end = this.end;
    run.url = this.url;
    return run;
  }
}

export class TextStyleSpan {
  public static readonly FLAG_STYLE_BOLD = 1;
  public static readonly FLAG_STYLE_ITALIC = 2;
  public static readonly FLAG_STYLE_MONO = 4;
  public static readonly FLAG_STYLE_STRIKE = 8;
  public static readonly FLAG_STYLE_UNDERLINE = 16;
  public static readonly FLAG_STYLE_SPOILER = 32;
  public static readonly FLAG_STYLE_BLOCKQUOTE = 64;
  public static readonly FLAG_STYLE_PRE = 128;
  public static readonly FLAG_STYLE_URL = 256;

  private style: TextStyleRun;

  constructor(styleRun: TextStyleRun) {
    this.style = styleRun;
  }

  public getTextStyleRun(): TextStyleRun {
    return this.style;
  }

  public isBold(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_BOLD) !== 0;
  }

  public isItalic(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_ITALIC) !== 0;
  }

  public isMono(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_MONO) !== 0;
  }

  public isPre(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_PRE) !== 0;
  }

  public isStrike(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_STRIKE) !== 0;
  }

  public isUnderline(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_UNDERLINE) !== 0;
  }

  public isSpoiler(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_SPOILER) !== 0;
  }

  public isBlockquote(): boolean {
    return (this.style.flags & TextStyleSpan.FLAG_STYLE_BLOCKQUOTE) !== 0;
  }

  public getUrl(): string | undefined {
    return this.style.url;
  }
}
