import clsx from "clsx";
import React from "react";
import { cn } from "~/lib/utils";
import { Slot } from "@radix-ui/react-slot";
import { cva, VariantProps } from "class-variance-authority";

function getCorrespondingFontForLetter(letter: string): "font-times" | "font-cooper" | "font-earwig" | "font-expose" {
    switch (letter) {
        case "A":
        case "I":
        case "L":
        case "M":
        case "W":
        case "X":
        case "a":
        case "j":
        case "l":
        case "r":
        case "p":
        case "5":
        case "6":
        case "9":
        case "1":
        case "0":
        case "$":
            return "font-times";

        case "C":
        case "F":
        case "J":
        case "N":
        case "P":
        case "R":
        case "T":
        case "V":
        case "Z":
        case "b":
        case "c":
        case "e":
        case "i":
        case "k":
        case "m":
        case "o":
        case "q":
        case "u":
        case "x":
        case "y":
        case "4":
        case "7":
        case "@":
        case "%":
        case "^":
        case "(":
        case ")":
            return "font-earwig";
        
        case "B":
        case "E":
        case "G":
        case "H":
        case "O":
        case "Q":
        case "S":
        case "U":
        case "d":
        case "f":
        case "g":
        case "s":
        case "w":
        case "z":
        case "3":
        case "8":
        case "!":
            return "font-expose";

        case "D":
        case "K":
        case "Y":
        case "h":
        case "n":
        case "t":
        case "v":
        case "2":
        case "#":
            return "font-cooper";

        default:
            return "font-times";
    }
}

interface NewspaperLetterProps extends React.HTMLAttributes<HTMLSpanElement>
{
    letter: string,
    asChild?: boolean
}

export function NewspaperLetter({ letter, className, asChild, ...props }: NewspaperLetterProps) {
    const Comp = asChild ? Slot : "span";

    const correspondingFont = getCorrespondingFontForLetter(letter);

    return (
        <Comp className={cn("relative", correspondingFont, className)} {...props}>
            {letter}
        </Comp>
    );
}

const NewspaperTextVariants = cva(
    "tracking-tight",
    {
        variants: {
            palette: {
                whiteOnRed: "text-white bg-red",
                whiteOnBlack: "text-white bg-black",
                blackOnWhite: "text-black bg-white",
                whiteOnTransparent: "text-white bg-transparent",
                blackOnTransparent: "text-black bg-transparent",
                redOnTransparent: "text-red bg-transparent"
            }
        },
        defaultVariants: {
            palette: "whiteOnBlack"
        }
    }
);

const LETTER_MIN_BOTTOM_OFFSET = 0.025, LETTER_MAX_BOTTOM_OFFSET = 0.125;

interface NewspaperTextProps extends VariantProps<typeof NewspaperTextVariants>
{
    text: string,
    redLetters?: string[],
    element?: "p" | "h1" | "h2" | "h3" | "h4" | "h5" | "h6" | "span" | "div",
    className?: string
}

export function NewspaperText({ text, redLetters = [], element = "p", className, palette, ...props }: NewspaperTextProps) {
    const Comp = element;

    return (
        <Comp
            className={cn(
                NewspaperTextVariants({ palette }),
                className,
            )}
            {...props}
        >
            {text.split("").map((char, i) => {
                const bottomOffset = Math.random() * (LETTER_MAX_BOTTOM_OFFSET - LETTER_MIN_BOTTOM_OFFSET) + LETTER_MIN_BOTTOM_OFFSET;
                
                return (
                    <NewspaperLetter
                        key={`${text}-char@${i}-${char}`}
                        letter={char}
                        className={clsx({ "text-red" : redLetters.includes(char) })}
                        style={{
                            "bottom": `${bottomOffset}em`
                        }}
                    />
                );
            })}
        </Comp>
    );
}