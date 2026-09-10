import {
  AppNav,
  Avatar,
  AvatarFallback,
  AvatarImage,
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  EmptyState,
  Input,
  Modal,
  ModalContent,
  ModalDescription,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  PictogramMark,
  Spinner,
  Textarea,
  Toast,
  ToastClose,
  ToastDescription,
  ToastProvider,
  ToastTitle,
  ToastViewport,
} from '@shared';
import { ImageOff, LogOut, Settings, UserRound } from 'lucide-react';
import { type ReactNode, useState } from 'react';

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-xs font-semibold uppercase tracking-[0.08em] text-foreground-subtle">
        {title}
      </h2>
      <div className="flex flex-wrap items-start gap-4">{children}</div>
    </section>
  );
}

function Swatch({ name, className }: { name: string; className: string }) {
  return (
    <div className="flex w-40 items-center gap-3">
      <div className={`size-10 rounded-control border border-border ${className}`} />
      <span className="text-sm text-foreground-muted">{name}</span>
    </div>
  );
}

export function UiShowcase() {
  return (
    <div className="mx-auto flex min-h-dvh max-w-4xl flex-col gap-12 px-6 py-12">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">shared/ui showcase</h1>
        <p className="text-sm text-foreground-muted">
          Every primitive in its states, on the design tokens. Snapshot target for the
          visual-regression suite.
        </p>
      </header>

      <Section title="Brand">
        <div className="flex items-center gap-4 text-foreground">
          <PictogramMark className="size-6" />
          <PictogramMark className="size-10" />
          <PictogramMark className="size-16" />
        </div>
        <div className="flex items-center gap-2 text-foreground">
          <PictogramMark className="size-6" />
          <span className="text-xl font-semibold tracking-tight">Pictogram</span>
        </div>
      </Section>

      <Section title="AppNav">
        <div className="w-full overflow-hidden rounded-card border border-border">
          <AppNav
            viewer={{ username: 'ansel' }}
            onSignOut={() => {
              // inert in the showcase
            }}
          />
        </div>
      </Section>

      <Section title="Surfaces & lines">
        <Swatch name="canvas" className="bg-canvas" />
        <Swatch name="surface" className="bg-surface" />
        <Swatch name="surface-muted" className="bg-surface-muted" />
        <Swatch name="border" className="bg-border" />
        <Swatch name="border-strong" className="bg-border-strong" />
      </Section>

      <Section title="Accent, danger & status">
        <Swatch name="accent" className="bg-accent" />
        <Swatch name="accent-hover" className="bg-accent-hover" />
        <Swatch name="danger" className="bg-danger" />
        <Swatch name="danger-surface" className="bg-danger-surface" />
        <Swatch name="success" className="bg-success" />
        <Swatch name="ring" className="bg-ring" />
        <Swatch name="like" className="bg-like" />
      </Section>

      <Section title="Type scale">
        <div className="flex flex-col gap-2">
          <p className="text-3xl font-semibold tracking-tight">Display · text-3xl</p>
          <p className="text-2xl font-semibold tracking-tight">Title · text-2xl</p>
          <p className="text-xl font-semibold tracking-tight">Heading · text-xl</p>
          <p className="text-lg font-semibold">Subhead · text-lg</p>
          <p className="text-base">Body · text-base — the caption reads at this size.</p>
          <p className="text-sm text-foreground-muted">Small · text-sm — secondary detail.</p>
          <p className="text-xs text-foreground-subtle">Caption · text-xs — 3 hours ago</p>
        </div>
      </Section>

      <Section title="Button — variants">
        <Button>Primary</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="danger">Delete</Button>
        <Button variant="link">Link</Button>
      </Section>

      <Section title="Button — sizes, loading, disabled">
        <Button size="sm">Small</Button>
        <Button size="md">Medium</Button>
        <Button size="lg">Large</Button>
        <Button loading>Publishing</Button>
        <Button disabled>Disabled</Button>
        <Button size="icon" aria-label="Settings">
          <Settings />
        </Button>
      </Section>

      <Section title="Input & Textarea">
        <div className="flex w-64 flex-col gap-3">
          <Input placeholder="Username" />
          <Input defaultValue="ansel" />
          <Input aria-invalid defaultValue="taken" />
          <Input disabled placeholder="Disabled" />
          <Textarea placeholder="Write a caption…" />
        </div>
      </Section>

      <Section title="Avatar">
        <Avatar size="sm">
          <AvatarFallback>AL</AvatarFallback>
        </Avatar>
        <Avatar size="md">
          <AvatarFallback>AL</AvatarFallback>
        </Avatar>
        <Avatar size="lg">
          <AvatarImage
            src="data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' width='112' height='112'%3E%3Crect width='112' height='112' fill='%236366f1'/%3E%3C/svg%3E"
            alt="Ansel"
          />
          <AvatarFallback>AL</AvatarFallback>
        </Avatar>
      </Section>

      <Section title="Card">
        <Card className="w-80">
          <CardHeader>
            <CardTitle>Golden hour</CardTitle>
            <CardDescription>Posted 3 hours ago</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm">A caption sits in the card body, on the surface token.</p>
          </CardContent>
          <CardFooter>
            <Button size="sm">Open</Button>
            <Button size="sm" variant="secondary">
              Share
            </Button>
          </CardFooter>
        </Card>
      </Section>

      <Section title="Modal">
        <ModalDemo />
      </Section>

      <Section title="DropdownMenu">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="secondary">Account</Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuLabel>@ansel</DropdownMenuLabel>
            <DropdownMenuItem>
              <UserRound /> Your profile
            </DropdownMenuItem>
            <DropdownMenuItem>
              <Settings /> Edit profile
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem>
              <LogOut /> Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </Section>

      <Section title="Toast">
        <ToastProvider>
          <div className="flex w-full max-w-sm flex-col gap-2">
            <Toast open variant="success">
              <div className="flex flex-col gap-1">
                <ToastTitle>Posted</ToastTitle>
                <ToastDescription>Your photo is live.</ToastDescription>
              </div>
              <ToastClose />
            </Toast>
            <Toast open variant="error">
              <div className="flex flex-col gap-1">
                <ToastTitle>Something went wrong</ToastTitle>
                <ToastDescription>That didn&rsquo;t work. Try again.</ToastDescription>
              </div>
              <ToastClose />
            </Toast>
            <ToastViewport className="!static w-full" />
          </div>
        </ToastProvider>
      </Section>

      <Section title="Spinner">
        <Spinner size="sm" />
        <Spinner size="md" />
        <Spinner size="lg" label="Loading" />
      </Section>

      <Section title="EmptyState">
        <div className="w-96 rounded-card border border-border">
          <EmptyState
            icon={ImageOff}
            title="No posts yet"
            description="When you follow people, their posts show up here."
            action={<Button size="sm">Find people</Button>}
          />
        </div>
      </Section>
    </div>
  );
}

function ModalDemo() {
  const [isOpen, setIsOpen] = useState(false);
  const close = () => {
    setIsOpen(false);
  };
  return (
    <>
      <Button
        variant="danger"
        onClick={() => {
          setIsOpen(true);
        }}
      >
        Delete post…
      </Button>
      <Modal isOpen={isOpen} onOpenChange={setIsOpen}>
        <ModalContent role="alertdialog">
          <ModalHeader>
            <ModalTitle>Delete this post?</ModalTitle>
            <ModalDescription>
              It leaves your grid and everyone&rsquo;s feed. This can&rsquo;t be undone.
            </ModalDescription>
          </ModalHeader>
          <ModalFooter>
            <Button variant="secondary" onClick={close}>
              Cancel
            </Button>
            <Button variant="danger" onClick={close}>
              Delete
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  );
}
